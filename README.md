# Functional Requirements/Attributes
## Clients
- [ ] Clients should only need to communicate with a central server to find peers and facilitate [hole-punching](https://github.com/KunjanThadani/holepunchsample/) (or assume that all clients have a listening port forwarded[^1]).
- [ ] Messages between clients should *never* go through any sort of central or relay server.
- [ ] All messages should be encrypted to facilitate secure transfer of data.
- [ ] Clients should be able to pair with each other to start messaging via a user ID
- [ ] For simplicity, users should be per-device, i.e. each client installation has a different user ID and messages do not sync between one person's devices if they use multiple.
- [ ] Clients should attempt to contact the central server frequently to ensure the client's IP in the server's database is up-to-date.
- [ ] Clients should have a local database with multiple tables for user and message caching.
	- [ ] One table for known user hashes, as well as a nickname for each user.
	- [ ] One for messages which foreign key reference the known users table's ID column.
- [ ] The first time the client process is opened, it should connect to the server to receive a unique user hash.
### Optional
- [ ] Clients should be able to pair using a QR code or nearby device scanning.
- [ ] Clients should be able to send small files or images to each other.
- [ ] Clients get rid of cached messages after a user-defined amount of time.
	- [ ] By definition requires adding a posted date column to the message tables.

## Server
- [ ] The server should have a database with at least a table that holds a user ID and the corresponding IP.
- [ ] The server should support multiple user connections at once to be scale-able (for testing purposes, 2-4 is sufficient).
- [ ] The first time a new client connects, a new user hash should be generated and added to the user table, and then the hash should be sent back to the user.
- [ ] The server should accept requests from clients to contact other clients and facilitate their attempt at connecting to each other via hole-punching.

# Non-Functional Requirements/Attributes
- [x] Clients' local databases should use something lightweight such as SQLite
- [x] Server's database should use something more reliable like MySQL

# Database Tables
## Clients
### User Table

| ID  |            User Hash             | Nickname  |
| :-: | :------------------------------: | --------- |
|  1  | e2d38e18759ea68bace101ab39d62e41 | Andrea    |
|  2  | 1c1eb4329e9b82168baaaa84b065209f | Travis    |
|  3  | 3889ede791c6872162ed0ab8ce513cd7 | Spam Risk |

>Clients should store their own user hash cached somewhere.

### Message Table

| Message ID | User ID (FK) | IsAuthor |                        Message Contents                         | File (Optional) |
| :--------: | :----------: | :------: | :-------------------------------------------------------------: | :-------------: |
|     1      |      1       |  false   |                             "Hello"                             |      null       |
|     2      |      1       |   true   |                              "Hi"                               |      null       |
|     3      |      1       |  false   |                       "Check out my cat"                        |   catPic.jpg    |
|     4      |      1       |   true   |                           "Cool cat"                            |      null       |
|     5      |      3       |  false   | "You or a loved one may be entitled to financial compensation." |      null       |

## Server
### User Table
| ID  |            User Hash             | Last-known IP |
| :-: | :------------------------------: | :-----------: |
|  1  | e2d38e18759ea68bace101ab39d62e41 |  64.82.8.109  |
|  2  | 1c1eb4329e9b82168baaaa84b065209f |  161.6.1.200  |
|  3  | 3889ede791c6872162ed0ab8ce513cd7 | 102.184.21.14 |

>It should be sufficient to generate user IDs by passing some sort of randomly generated string[^2] into an [MD5 hashing function](https://www.geeksforgeeks.org/md5-hash-in-java/). This will allow 16^32 total users (definitely overkill, but not really a problem).

[^1]: If we go down this route, the user tables on the client side should also have the last-known IP for each client, so that peers can attempt to connect to each other without needing to check with a central server; then, if that fails, it can check with the central server to see if the IP of the user they are trying to connect to has changed.
[^2]: For example, the following code:
	```java
	protected String getSaltString() {
		String SALTCHARS = "abcdefghijklmnopqrstuvwxyz1234567890";
		StringBuilder salt = new StringBuilder();
		SecureRandom rnd = new SecureRandom();
		while (salt.length() < 32) { // length of the random string.
			int index = (int) (rnd.nextFloat() * SALTCHARS.length());
			salt.append(SALTCHARS.charAt(index));
		}
		String saltStr = salt.toString();
		return saltStr;
	}