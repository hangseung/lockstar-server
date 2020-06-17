package com.app.lockstar;

import javax.persistence.*;

@Entity
public class File {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(name = "FILE_KEY", nullable = false)
    private String key;

    @Column(name = "OWNER_USER_ID", nullable = false)
    private Integer ownerUserId;

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getKey() {
        return key;
    }

    public Integer getOwnerUserId() {
        return ownerUserId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setOwnerUserId (Integer ownerUserId) {
        this.ownerUserId = ownerUserId;
    }
}
