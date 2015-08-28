# ActiveStack Sync Engine

## Authentication

ActiveStack provides several authentication mechanisms straight out of the box: anonymous,
Google and File based. We call these "Auth Providers".

### Usage

In order to authenticate against one of these authentication options you'll need to send an 'authenticate' message
to the server with content body looking something like:

```javascript
{
    cn: "com.percero.agents.auth.vo.AuthenticationRequest",
    credential: "A String that represents your credential...provider specific format."
    authProvider: "<AUTH_PROVIDER_ID>"
}
```

This message tells the server you want to authenticate with a specific authentication mechanism and passes the
credential that should be used to authenticate the user against the auth provider.

Each provider has a unique provider ID and potentially has a unique credential format. Below is a table of the
providers that ActiveStack currently ships with:

| Provider      | ID            |
| Anonymous     | 'anonymous'   |
| Google OAuth2 | 'googleoauth' |
| JSON File     | 'file'        |

For more specifics about individual auth providers see their details below.

### JSON File Auth Provider

**WARNING** This authentication method should be used for development only and never in production.

ActiveStack provides the ability to setup simple authentication via a JSON text file. In order
to enable it, you'll need to add a configuration property to your application properties file.

```
fileAuth.fileLocation=/some/place/on/disk/users.json
```

This JSON file should have the following format:

```json
[
    {
        "firstName":"Jonathan",
        "lastName":"Samples",
        "email":"jonathan@activestack.io",
        "passHash":"3da541559918a808c2402bba5012f6c60b27661c" // SHA1 Hash of 'asdf'
    }
]
```

In order to create password hashes you'll need to use a sha1 encoder like http://www.sha1-online.com/

To authenticate using this provider you'll have to provide a credential with the following format

```
<EMAIL>:<PASSWORD>
```
EMAIL - The plain text email address listed in the file
PASSWORD - The plain text password that should be hashed to compare with the `passHash` in the JSON file.

### Google OAuth2 Auth Provider

**WRITE ME**

### Anonymous Auth Provider

**WRITE ME**
