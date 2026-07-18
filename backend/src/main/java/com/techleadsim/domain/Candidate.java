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

    // Eager: the strengths list is tiny (a handful of rows per candidate, 4 candidates total)
    // and is read on every candidate lineup response, which is built outside a transaction
    // (open-in-view is disabled) — lazy loading here would throw LazyInitializationException.
    @ElementCollection(fetch = FetchType.EAGER)
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
