package org.example.enteties;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.example.utils.ormannotation.Column;
import org.example.utils.ormannotation.Table;

@ToString
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Table(name = "Example")
public class Person {

    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name;
}
