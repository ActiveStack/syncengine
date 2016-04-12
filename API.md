

# The Basics
At the heart of the ActiveStack API are [`SyncRequest`'s](https://github.com/ActiveStack/syncengine/blob/156ea8927fba9681f8b547662904073a45f990b1/src/main/java/com/percero/agents/sync/vo/SyncRequest.java) and [`SyncResponse`'s](https://github.com/ActiveStack/syncengine/blob/156ea8927fba9681f8b547662904073a45f990b1/src/main/java/com/percero/agents/sync/vo/SyncResponse.java).  A specialized `SyncRequest` is sent to the ActiveStack Sync Engine and a `SyncResponse` is sent back the two being linked together by the `SyncResponse`.`correspondingMessageId`, which is the `ID` of the `SyncRequest`.

## Login
Login is perhaps the most complicated of the API's in that it is meant to be generic and flexible enough to handle any type of authentication (OAuth, username/password, custom, etc).  The main authentication engine is [`AuthService2`](https://github.com/ActiveStack/syncengine/blob/156ea8927fba9681f8b547662904073a45f990b1/src/main/java/com/percero/agents/auth/services/AuthService2.java).

Login basically accepts some form of user credentials and returns a `UserID` and `ClientID` upon successful authentication.  The `ClientID` is the main way that ActiveStack knows who the user is and also enables a single user to be simultaneously logged in on multipled devices.

A successful login returns a [`UserToken`](https://github.com/ActiveStack/syncengine/blob/156ea8927fba9681f8b547662904073a45f990b1/src/main/java/com/percero/agents/auth/vo/UserToken.java) object that contains the [`User`](https://github.com/ActiveStack/syncengine/blob/156ea8927fba9681f8b547662904073a45f990b1/src/main/java/com/percero/agents/auth/vo/User.java), `clientId`, `deviceId`, and `token` (required for reauthentication). Typically, upon successful login, the app will issue a `findByExample` request for the class that represents the user in the application.  For example, if `Person` is the class that represents the user, the app would issue a `findByExample` request with `theObject` payload:
```
{
  "cn": "com.app.mo.Person",  // Or whatever the actual class name of the `Person` object is
  "userId": <UserToken.User.ID>,  // The `UserToken`.`User`.`ID` from the `AuthenticationResponse`
}
```
This should result in the `Person` object identified by that User.ID to be returned by the ActiveStack SyncEngine.

### [authenticate](https://github.com/ActiveStack/syncengine/blob/156ea8927fba9681f8b547662904073a45f990b1/src/main/java/com/percero/agents/auth/services/AuthService2.java#L47)
- Authenticates a user by their credentials.
  - Request: [`AuthenticationRequest`](https://github.com/ActiveStack/syncengine/blob/156ea8927fba9681f8b547662904073a45f990b1/src/main/java/com/percero/agents/auth/vo/AuthenticationRequest.java)
  - Response: [`AuthenticationResponse`](https://github.com/ActiveStack/syncengine/blob/156ea8927fba9681f8b547662904073a45f990b1/src/main/java/com/percero/agents/auth/vo/AuthenticationResponse.java)
  - Parameters:
    - `authProvider`: This is the name of the auth provider that will be used for authentication.  The auth provider can either be built-in auth providers, or a custom defined auth provider.
    - `credential`:  This is a string that represents the user's credentials.  Based on the authentication provider, this string is parsed accordingly to pull out the various parts of the credentials.
      - Examples:
        - [`BasicAuthCredential`](https://github.com/ActiveStack/syncengine/blob/156ea8927fba9681f8b547662904073a45f990b1/src/main/java/com/percero/agents/auth/vo/BasicAuthCredential.java): Handled by [`InMemoryAuthProvider`](https://github.com/ActiveStack/syncengine/blob/156ea8927fba9681f8b547662904073a45f990b1/src/main/java/com/percero/agents/auth/services/InMemoryAuthProvider.java)
        - [`OAuthCredential`](https://github.com/ActiveStack/syncengine/blob/156ea8927fba9681f8b547662904073a45f990b1/src/main/java/com/percero/agents/auth/vo/OAuthCredential.java): Handled by [`GoogleAuthProvider`](https://github.com/ActiveStack/syncengine/blob/156ea8927fba9681f8b547662904073a45f990b1/src/main/java/com/percero/agents/auth/services/GoogleAuthProvider.java)
        - AnonCredential: Handled by [`AnonAuthProvider`](https://github.com/ActiveStack/syncengine/blob/156ea8927fba9681f8b547662904073a45f990b1/src/main/java/com/percero/agents/auth/services/AnonAuthProvider.java)

### [reauthenticate](https://github.com/ActiveStack/syncengine/blob/156ea8927fba9681f8b547662904073a45f990b1/src/main/java/com/percero/agents/auth/services/AuthService2.java#L83)
- Reauthenticates a user by a token (which is typically assigned upon successful authentication).
  - Request: [`ReauthenticationRequest`](https://github.com/ActiveStack/syncengine/blob/156ea8927fba9681f8b547662904073a45f990b1/src/main/java/com/percero/agents/auth/vo/ReauthenticationRequest.java)
  - Response: [`AuthenticationResponse`](https://github.com/ActiveStack/syncengine/blob/156ea8927fba9681f8b547662904073a45f990b1/src/main/java/com/percero/agents/auth/vo/AuthenticationResponse.java)
  - Parameters:
    - `authProvider`: This is the name of the auth provider that will be used for authentication.  The auth provider can either be built-in auth providers, or a custom defined auth provider.
    - `token`:  This is the string token that was returned upon successful authentication.

## Core API

- All `className` references assume that the corresponding class is part of the registered data model, meaning it is included in the `ActiveStack.Domain` module.

### [findById](https://github.com/ActiveStack/syncengine/blob/156ea8927fba9681f8b547662904073a45f990b1/src/main/java/com/percero/amqp/handlers/FindByIdHandler.java)
- Retrieves the object identified by a `className` and an `ID`.  The server will respond with a `findByIdResponse` containing either the result, or a NULL result indicating the specified object was not found, and registers the Client for any updates to that object.
  - Request: [`FindByIdRequest`](https://github.com/ActiveStack/syncengine/blob/master/src/main/java/com/percero/agents/sync/vo/FindByIdRequest.java)
  - Response: [`FindByIdResponse`](https://github.com/ActiveStack/syncengine/blob/master/src/main/java/com/percero/agents/sync/vo/FindByIdResponse.java)
  - Parameters:
    - `theClassName`: The name of the class
    - `theClassId`: The ID of the object to find
  - Note that the API should be able to handle inheritance.  So "parentClass::ID" and "class::ID" should both return the same result (assuming that "class" inherits from "parentClass").

### [findByIds](https://github.com/ActiveStack/syncengine/blob/156ea8927fba9681f8b547662904073a45f990b1/src/main/java/com/percero/amqp/handlers/FindByIdsHandler.java)
- Retrieves a list of object identified by the `className` and a list of `ID`'s and registers the Client for any updates to those objects.
  - Request: [`FindByIdsRequest`](https://github.com/ActiveStack/syncengine/blob/master/src/main/java/com/percero/agents/sync/vo/FindByIdsRequest.java)
  - Response: [`FindByIdsResponse`](https://github.com/ActiveStack/syncengine/blob/master/src/main/java/com/percero/agents/sync/vo/FindByIdsResponse.java)
  - Parameters:
    - `theClassIdList`: [`ClassIDPairs`](https://github.com/ActiveStack/syncengine/blob/156ea8927fba9681f8b547662904073a45f990b1/src/main/java/com/percero/agents/sync/vo/ClassIDPairs.java) object which contains the `className` and a list of `ID`'s to retrieve
  - Note that the API should be able to handle inheritance.  So "parentClass::ID" and "class::ID" should both return the same result (assuming that "class" inherits from "parentClass").

### [getAllByName](https://github.com/ActiveStack/syncengine/blob/156ea8927fba9681f8b547662904073a45f990b1/src/main/java/com/percero/amqp/handlers/GetAllByNameHandler.java)
- Retrieves all objects of a particular class and registers the Client for any updates to those objects.
  - Request: [`GetAllByNameRequest`](https://github.com/ActiveStack/syncengine/blob/156ea8927fba9681f8b547662904073a45f990b1/src/main/java/com/percero/agents/sync/vo/GetAllByNameRequest.java)
  - Response: [`GetAllByNameResponse`](https://github.com/ActiveStack/syncengine/blob/156ea8927fba9681f8b547662904073a45f990b1/src/main/java/com/percero/agents/sync/vo/GetAllByNameResponse.java)
  - Parameters:
    - `theClassName`: The name of the class to get all objects.
    - `pageSize` (optional): Number of items to return in result.
    - `pageNumber` (optional): Desired page number
    - `returnTotal` (optional): If set to `true`, returns the total number of objects to be returned.  Typically used in the first call to determine exactly how many objects are expected.

### [findByExample](https://github.com/ActiveStack/syncengine/blob/156ea8927fba9681f8b547662904073a45f990b1/src/main/java/com/percero/amqp/handlers/FindByExampleHandler.java)
- Retrieves all objects that match the supplied sample object and registers the Client for any updates to those objects.
  - Request: [`FindByExampleRequest`](https://github.com/ActiveStack/syncengine/blob/156ea8927fba9681f8b547662904073a45f990b1/src/main/java/com/percero/agents/sync/vo/FindByExampleRequest.java)
  - Response: [`FindByExampleResponse`](https://github.com/ActiveStack/syncengine/blob/156ea8927fba9681f8b547662904073a45f990b1/src/main/java/com/percero/agents/sync/vo/FindByExampleResponse.java)
  - Parameters:
    - `theObject`: A sample object of the domain model.  Fields on the object that are set will be included as part of the filter criteria

### [putObject](https://github.com/ActiveStack/syncengine/blob/156ea8927fba9681f8b547662904073a45f990b1/src/main/java/com/percero/amqp/handlers/PutObjectHandler.java)
- Updates an existing object
  - Request: [`PutRequest`](https://github.com/ActiveStack/syncengine/blob/156ea8927fba9681f8b547662904073a45f990b1/src/main/java/com/percero/agents/sync/vo/PutRequest.java)
  - Response: [`PutResponse`](https://github.com/ActiveStack/syncengine/blob/156ea8927fba9681f8b547662904073a45f990b1/src/main/java/com/percero/agents/sync/vo/PutResponse.java)
  - Parameters:
    - `theObject`
    - `putTimestamp` (optional): The time this object was updated. This is used for conflict resolution
    - `transId` (optional): A client defined transaction ID. Mostly used for tracking of updates, does NOT enforce a database transaction.

### [createObject](https://github.com/ActiveStack/syncengine/blob/156ea8927fba9681f8b547662904073a45f990b1/src/main/java/com/percero/amqp/handlers/CreateObjectHandler.java)
- Creates a new object
  - Request: [`CreateRequest`](https://github.com/ActiveStack/syncengine/blob/156ea8927fba9681f8b547662904073a45f990b1/src/main/java/com/percero/agents/sync/vo/CreateRequest.java)
  - Response: [`CreateResponse`](https://github.com/ActiveStack/syncengine/blob/156ea8927fba9681f8b547662904073a45f990b1/src/main/java/com/percero/agents/sync/vo/CreateResponse.java)
  - Parameters:
    - `theObject`: The object to be created.  If the object does NOT contain an ID, the ActiveStack Sync Engine will create one.

### [removeObject](https://github.com/ActiveStack/syncengine/blob/156ea8927fba9681f8b547662904073a45f990b1/src/main/java/com/percero/amqp/handlers/RemoveObjectHandler.java)
- Removes an existing object
  - Request: [`RemoveRequest`](https://github.com/ActiveStack/syncengine/blob/156ea8927fba9681f8b547662904073a45f990b1/src/main/java/com/percero/agents/sync/vo/RemoveRequest.java)
  - Response: [`RemoveResponse`](https://github.com/ActiveStack/syncengine/blob/156ea8927fba9681f8b547662904073a45f990b1/src/main/java/com/percero/agents/sync/vo/RemoveResponse.java)
  - Parameters:
    - `removePair`: A [`ClassIDPair`](https://github.com/ActiveStack/syncengine/blob/156ea8927fba9681f8b547662904073a45f990b1/src/main/java/com/percero/agents/sync/vo/ClassIDPair.java) identifying the ID and class name of the object to be removed

### [getChangeWatcher](https://github.com/ActiveStack/syncengine/blob/156ea8927fba9681f8b547662904073a45f990b1/src/main/java/com/percero/amqp/handlers/GetChangeWatcherHandler.java)
- Retrieves the value of a ChangeWatcher and registers the Client for any updates to that value.
  - Request: [`PushCWUpdateRequest`](https://github.com/ActiveStack/syncengine/blob/master/src/main/java/com/percero/agents/sync/vo/PushCWUpdateRequest.java)
  - Response: [`PushCWUpdateResponse`](https://github.com/ActiveStack/syncengine/blob/master/src/main/java/com/percero/agents/sync/vo/PushCWUpdateResponse.java)
  - Parameters:
    - `classIdPair`: A [`ClassIDPair`](https://github.com/ActiveStack/syncengine/blob/156ea8927fba9681f8b547662904073a45f990b1/src/main/java/com/percero/agents/sync/vo/ClassIDPair.java) identifying the ID and class name of the object that the ChangeWatcher hangs off of.
    - `fieldName`: The name of the field that represents the ChangeWatcher.
    - `params` (optional): An array of strings that represent the parameters that uniquely identify the ChangeWatcher.

### [runServerProcess](https://github.com/ActiveStack/syncengine/blob/156ea8927fba9681f8b547662904073a45f990b1/src/main/java/com/percero/amqp/handlers/RunProcessHandler.java)
- Runs a custom server process.  This process can be a custom piece of code, a defined HTTP process, a defined SQL stored procedure, or some other defined Connector.
  - Request: [`RunServerProcessRequest`](https://github.com/ActiveStack/syncengine/blob/master/src/main/java/com/percero/agents/sync/vo/RunServerProcessRequest.java)
  - Response: [`RunServerProcessResponse`](https://github.com/ActiveStack/syncengine/blob/master/src/main/java/com/percero/agents/sync/vo/RunServerProcessResponse.java)
  - Parameters:
    - `queryName`: The name of the process.  To use a specific Connector (such as 'HTTP' or 'SQL_PROC' for database stored procedures), prefix the operation name of the Connector name and a ":".  Example:  "HTTP:fetchDataFromHttpEndpoint"
    - `queryArguments` (optional): Any required parameters for the server process.  Typically, this is passed as some sort of map (parameterName -> parameterValue)


