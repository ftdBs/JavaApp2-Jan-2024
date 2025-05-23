package org.csystem.app.information.server;

import com.karandev.io.util.console.Console;
import com.karandev.util.net.TcpUtil;
import com.karandev.util.net.exception.NetworkException;
import org.csystem.app.information.server.communication.client.CommunicationServerInfo;
import org.csystem.app.information.server.communication.global.CommunicationServerUtil;
import org.csystem.net.tcp.server.ConcurrentServer;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;

import static org.csystem.communication.library.common.CommunicationMessage.*;

public class JoinMeetingInformationServer implements Closeable {
    private static final int SOCKET_TIMEOUT = 4000;
    private final ConcurrentServer m_server;

    private int connectToCommunicationServerCallback(Socket socket, String id)
    {
        TcpUtil.sendStringViaLength(socket, id);
        var status = TcpUtil.receiveStringViaLength(socket);

        if (status.equals(SUCCESS))
            return TcpUtil.receiveInt(socket);

        return 0;
    }

    private int connectToCommunicationServers(String id, CommunicationServerInfo communicationServerInfo)
    {
        var port = 0;

        synchronized (CommunicationServerUtil.SYNC_SERVERS_OBJECT) {
            for (var ci : CommunicationServerUtil.SERVERS) {
                try (var socket = new Socket(ci.getHost(), ci.getPort())) {
                    port = connectToCommunicationServerCallback(socket, id);

                    if (port > 0) {
                        communicationServerInfo.setHost(ci.getHost());
                        communicationServerInfo.setPort(port); //Bu port numarası meeting started durumdayken de gönderilebilir? Düşünelim!...
                        break;
                    }
                }
                catch (IOException ex) {
                    Console.Error.writeLine("Message::%s", ex.getMessage());
                    port = -1;
                }
            }
        }

        return port;
    }

    private void handleClient(Socket socket)
    {
        try (socket) {
            socket.setSoTimeout(SOCKET_TIMEOUT);
            var id = TcpUtil.receiveStringViaLength(socket);
            var info = new CommunicationServerInfo();

            var port = connectToCommunicationServers(id, info);

            if (port > 0) {
                TcpUtil.sendStringViaLength(socket, SUCCESS_INFO);
                TcpUtil.sendStringViaLength(socket, "%s:%d".formatted(info.getHost(), port));
            }
            else if (port == 0)
                TcpUtil.sendStringViaLength(socket, ERROR_INVALID_ID);
            else
                TcpUtil.sendStringViaLength(socket, ERROR_INTERNAL);
        }
        catch (NetworkException ex) {
            Console.Error.writeLine("Join Meeting Information Server:Network Exception Occurred:%s", ex.getMessage());
        }
        catch (IOException ex) {
            Console.Error.writeLine("Join Meeting Information Server:IO Exception Occurred:%s", ex.getMessage());
        }
        catch (Throwable ex) {
            Console.Error.writeLine("Join Meeting Information Server:Exception Occurred:%s", ex.getMessage());
        }
    }

    public JoinMeetingInformationServer(int port, int backlog) throws IOException
    {
        m_server = ConcurrentServer.builder()
                .setPort(port)
                .setBacklog(backlog)
                .setBeforeAcceptRunnable(() -> Console.writeLine("Join meeting information server is waiting for a client on port:%d", port))
                .setClientSocketConsumer(this::handleClient)
                .build();
    }

    public void run()
    {
        m_server.start();
    }

    public void close()
    {
        m_server.stop();
    }
}
