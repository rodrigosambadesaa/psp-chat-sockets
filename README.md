# PSP · Chat multiusuario por sockets

Reimplementación completa de la práctica de chat de PSP: servidor multiusuario, nick único, aviso de conexiones/desconexiones, lista de usuarios y difusión de mensajes a todos los clientes, incluido el emisor.

El protocolo es textual UTF-8 y el servidor mantiene sesiones concurrentes en un `ConcurrentHashMap`. Los sockets y threads se cierran al abandonar el chat. Para servidores remotos el cliente usa la comprobación de conectividad adaptada del Gist MIT de Rodrigo Sambade: https://gist.github.com/rodrigosambadesaa/6f8e895de21c76b946bf70a1deffe05b. Localhost no necesita Internet.

## Protocolo
1. Cliente envía `NICK <nombre>`.
2. Servidor responde `OK` o `ERROR ...`.
3. Cliente envía `MSG <texto>` o `QUIT`.
4. Servidor publica `USERS`, `JOIN`, `LEAVE` y `MESSAGE`.

```bash
mvn verify
java -cp target/classes dev.rodrigosambade.chat.ChatServer 5000
java -cp target/classes dev.rodrigosambade.chat.ChatClient localhost 5000 Rodrigo
```
