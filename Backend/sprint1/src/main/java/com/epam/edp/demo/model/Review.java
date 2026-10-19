package com.epam.edp.demo.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "reviews")
@Getter
@Setter
@NoArgsConstructor
public class Review {

    @Id
    private String id;

    @Indexed
    private String tourId;

    private String userId;
    private String userName;
    private String userAvatarUrl;
    private Double rate;
    private String comment;
    private LocalDate reviewDate;
    private boolean hidden = false;

    @CreatedDate
    private LocalDateTime createdAt;
}
