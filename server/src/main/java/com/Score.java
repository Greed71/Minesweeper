package com;

import jakarta.persistence.*;

@Entity
public class Score {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int points;

    @ManyToOne
    private User user;

    public Score() {}

    public Score(int points, User user) {
        this.points = points;
        this.user = user;
    }

    public Long getId() { return id; }
    public int getPoints() { return points; }
    public User getUser() { return user; }

    public void setId(Long id) { this.id = id; }
    public void setPoints(int points) { this.points = points; }
    public void setUser(User user) { this.user = user; }
}

