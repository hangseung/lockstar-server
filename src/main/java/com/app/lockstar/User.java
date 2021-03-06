package com.app.lockstar;

import com.google.common.hash.Hashing;

import javax.persistence.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Entity
public class User {
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String password;

    @Column(name = "PUBLIC_KEY")
    private String publicKey;

    @Column(name = "ORIGINAL_PUBLIC_KEY_NAME")
    private String originalPublicKeyName;

    @ManyToMany
    @JoinTable(name = "USER_FILE")
    private List<File> file = new ArrayList<>();

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public List<File> getFile() {
        return file;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPassword(String password) {
        this.password = Hashing.sha256()
                .hashString(password, StandardCharsets.UTF_8)
                .toString();
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public void setOriginalPublicKeyName (String originalPublicKeyName) {
        this.originalPublicKeyName = originalPublicKeyName;
    }

    public Boolean isSamePassword(String password) {
        return this.password.equals(Hashing.sha256()
                .hashString(password, StandardCharsets.UTF_8)
                .toString());
    }

    public void addFile (File file) {
        this.file.add(file);
    }

    public Boolean hasFilePermission (Integer fileId) {
        for (File file : this.file) {
            if (file.getId().equals(fileId)) {
                return true;
            }
        }
        return false;
    }
}
