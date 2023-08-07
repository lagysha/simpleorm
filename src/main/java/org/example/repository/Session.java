package org.example.repository;

import lombok.SneakyThrows;
import org.example.utils.ormannotation.Column;
import org.example.utils.ormannotation.Table;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class Session {

    private static final String SELECT_FROM_TABLE_BY_ID = "select * from %s where id = ?";
    private static final String UPDATE_TABLE_BY_ID = "update %s %s where id = ?";
    private final DataSource dataSource;
    private final static HashMap<EntityKey<?>, Object> session = new HashMap<>();
    private final static HashMap<EntityKey<?>, Object[]> entityKeySnapshotCopies = new HashMap<>();

    public Session(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @SneakyThrows
    public <T> T findById(Class<T> type, Long id) {

        var key = new EntityKey<>(type, id);


        if (session.containsKey(key)) {
            System.out.println("No Query");
            return (T) session.get(key);
        }

        ResultSet retrievedEntity = createFindByIdStatement(type, id);

        T object = getObjectFromStatement(key, type, retrievedEntity);
        session.put(key, object);

        return object;
    }

    private <T> ResultSet createFindByIdStatement(Class<T> type, Long Id) throws SQLException {
        var source = dataSource.getConnection();
        var annonationTable = type.getAnnotation(Table.class);

        var sqlStatement = SELECT_FROM_TABLE_BY_ID.formatted(annonationTable.name());
        System.out.println(sqlStatement);

        var statement = source.prepareStatement(sqlStatement);
        statement.setLong(1, Id);
        return statement.executeQuery();
    }

    private static <T> T getObjectFromStatement(EntityKey<T> key, Class<T> type, ResultSet resultSet) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, SQLException {
        T objectSetter = type.getConstructor().newInstance();
        var snapshotCopy = new ArrayList<>();
        var fieldsValues = Arrays.stream(type
                        .getDeclaredFields()).filter(field -> field.getAnnotation(Column.class) != null)
                .peek(e -> e.setAccessible(true)).toArray(Field[]::new);

        resultSet.next();

        for (Field field : fieldsValues) {
            String setterName = "set" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
            Object fieldValue = resultSet.getObject(field.getAnnotation(Column.class).name());
            objectSetter.getClass()
                    .getDeclaredMethod(setterName, field.getType())
                    .invoke(objectSetter, fieldValue);
            snapshotCopy.add(fieldValue);
        }
        entityKeySnapshotCopies.put(key, snapshotCopy.toArray(Object[]::new));
        return objectSetter;
    }

    public void close() {
        entityKeySnapshotCopies.entrySet().stream().filter(this::hasChanged)
                .forEach(this::performUpdate);
    }

    private boolean hasChanged(Map.Entry<EntityKey<?>, Object[]> entityKeyEntry) {
        var object = session.get(entityKeyEntry.getKey());
        var copyFields = entityKeyEntry.getValue();
        var fields = Arrays.stream(object.getClass().getDeclaredFields()).filter(field -> field.getAnnotation(Column.class) != null).peek(e -> e.setAccessible(true)
        ).map(e -> {
            try {
                return e.get(object);
            } catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            }
        }).toArray();

        for (int i = 0; i < fields.length; i++) {
            if (!fields[i].equals(copyFields[i])) {
                return true;
            }
        }
        return false;
    }

    @SneakyThrows
    private void performUpdate(Map.Entry<EntityKey<?>, Object[]> entityKeyEntry) {
        var sourceConnection = dataSource.getConnection();
        var annotation = entityKeyEntry.getKey().type().getAnnotation(Table.class);
        var fieldsValues = Arrays.stream(session.get(entityKeyEntry.getKey()).getClass()
                .getDeclaredFields()).filter(field -> field.getAnnotation(Column.class) != null)
                .peek(e -> e.setAccessible(true)).toArray(Field[]::new);


        String sqlStatement = createUpdateStatement(entityKeyEntry, annotation, fieldsValues);
        System.out.println(sqlStatement);

        var statement = sourceConnection.prepareStatement(sqlStatement);
        statement.setLong(1, (Long) entityKeyEntry.getKey().id());
        statement.executeUpdate();
    }

    private static String createUpdateStatement(Map.Entry<EntityKey<?>, Object[]> entityKeyEntry, Table annotation, Field[] fieldsValues) throws IllegalAccessException {
        StringBuilder statementBuilder = new StringBuilder("set ");

        for (int i = 0; i < fieldsValues.length; i++) {
            statementBuilder.append(fieldsValues[i].getAnnotation(Column.class).name())
                    .append(" = ")
                    .append(entityKeyEntry.getValue()[i].getClass().equals(String.class)
                            ? "'" + fieldsValues[i].get(session.get(entityKeyEntry.getKey())) + "', "
                            : entityKeyEntry.getValue()[i] + ", ");
        }

        return UPDATE_TABLE_BY_ID.formatted(annotation.name(),statementBuilder.substring(0, statementBuilder.length() - 2));
    }
}