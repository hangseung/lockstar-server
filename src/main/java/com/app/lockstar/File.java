package com.app.lockstar;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
public class File {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(name = "ORIGINAL_FILE_NAME", nullable = false)
    private String originalFileName;

    @Column(name = "FILE_KEY", nullable = false)
    private String key;

    @Column(name = "ORIGINAL_FILE_KEY_NAME", nullable = false)
    private String originalFileKeyName;

    @Column(name = "OWNER_USER_ID", nullable = false)
    private Integer ownerUserId;

    @Column(updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getOriginalFileName () {
        return originalFileKeyName;
    }

    public String getKey() {
        return key;
    }

    public Integer getOwnerUserId() {
        return ownerUserId;
    }

    public String getCreatedAtString () {
        return createdAt.toString();
    }

    public String getUpdatedAtString () {
        return updatedAt.toString();
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setOriginalFileName (String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setOriginalFileKeyName (String originalFileKeyName) {
        this.originalFileKeyName = originalFileKeyName;
    }

    public void setOwnerUserId (Integer ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public Boolean isExpired () {
        LocalDateTime current = LocalDateTime.now();
        return current.isAfter(createdAt.plusMonths(1));
    }
}
