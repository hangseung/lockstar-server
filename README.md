# lockstar-server

# Register New User

**URL** : `/user/add/`

**Method** : `POST`

**Data constraints**

```json
{
    "username": "[string]",
    "password": "[string]"
}
```

# Register User Public Key

**URL** : `/user/key/`

**Method** : `POST`

**Data constraints**

```json
{
    "username": "[string]",
    "password": "[string]",
    "key": "[file]"
}
```

# Download Server Public Key

**URL** : `/user/server_public_key/`

**Method** : `GET`

**Data constraints**

```json
{
    "username": "[string]",
    "password": "[string]"
}
```

# Upload File and File Key

**URL** : `/file/`

**Method** : `POST`

**Data constraints**

```json
{
    "username": "[string]",
    "password": "[string]",
    "file": "[file]",
    "file_key": "[file]"
}
```

# Replace File

**URL** : `/file/{file_id}`

**Method** : `POST`

**Data constraints**

```json
{
    "username": "[string]",
    "password": "[string]",
    "file": "[file]"
}
```

# Download File

**URL** : `/file/{file_id}`

**Method** : `GET`

**Data constraints**

```json
{
    "username": "[string]",
    "password": "[string]"
}
```

# Download File Key

**URL** : `/file/key/{file_id}/`

**Method** : `GET`

**Data constraints**

```json
{
    "username": "[string]",
    "password": "[string]"
}
```


# Allow Users to Download the File and Key

**URL** : `/file/allow/{file_id}/`

**Method** : `POST`

**Data constraints**

```json
{
    "username": "[string]",
    "password": "[string]",
    "usernames": "[string(usernames separated with comma)]"
}
```

# Get Downloadable File List

**URL** : `/file/list/`

**Method** : `GET`

**Data constraints**

```json
{
    "username": "[string]",
    "password": "[string]"
}
```
