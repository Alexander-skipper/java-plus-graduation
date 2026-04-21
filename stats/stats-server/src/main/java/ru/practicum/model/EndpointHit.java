package ru.practicum.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "endpoint_hits")

public class EndpointHit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "app")
    private String app;

    @NotNull
    @Column(name = "uri")
    private String uri;

    @NotNull
    @Column(name = "user_ip")
    private String ip;

    @NotNull
    @Column
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

}
