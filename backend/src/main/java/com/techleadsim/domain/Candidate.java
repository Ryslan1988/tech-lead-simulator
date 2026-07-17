package com.techleadsim.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "candidate")
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String role;
    @Column(name = "avatar_url")
    private String avatarUrl;
    private int slot;

    @ElementCollection
    @CollectionTable(name = "candidate_strength", joinColumns = @JoinColumn(name = "candidate_id"))
    @Column(name = "strength")
    private List<String> strengths = new ArrayList<>();

    protected Candidate() {}

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getRole() { return role; }
    public String getAvatarUrl() { return avatarUrl; }
    public int getSlot() { return slot; }
    public List<String> getStrengths() { return strengths; }
}
