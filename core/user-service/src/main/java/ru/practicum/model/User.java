package ru.practicum.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.validator.constraints.Length;

@Entity
@Table(name = "users")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Email
    @Length(min = 6, max = 254)
    @NotBlank
    @Column
    private String email;

    @Length(min = 2, max = 250)
    @NotBlank
    @Column
    private String name;

    @Override
    public String toString() {
        return "User{" + "id=" + id + ", email=" + email + ", name=" + name + '}';
    }
}
