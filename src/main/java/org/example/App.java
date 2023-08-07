package org.example;

import org.example.enteties.Person;
import org.example.repository.Session;
import org.postgresql.ds.PGSimpleDataSource;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        PGSimpleDataSource dataSource = createDataSource("postgres","postgres","jdbc:postgresql://localhost/example");
        Session session = new Session(dataSource);
        Person person = session.findById(Person.class,1L);
        System.out.println("person = " + person);
        Person theSamePerson = session.findById(Person.class,1L);
        System.out.println("theSamePerson = " + theSamePerson);
        person.setName("Person");
        System.out.println(person == theSamePerson);
        session.close();
    }

    private static PGSimpleDataSource createDataSource(String username, String password, String URl) {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(URl);
        dataSource.setUser(username);
        dataSource.setPassword(password);
        return dataSource;
    }
}
