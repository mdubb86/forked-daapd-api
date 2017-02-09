package com.meridian.dacp.service;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DacpService {

    public final Pattern BRANCHES = Pattern.compile("(cmst|mlog|agal|mlcl|mshl|mlit|abro|abar|apso|caci|avdb|cmgt|aply|adbs|casp|mdcl)");
    public final Pattern STRINGS = Pattern.compile("(minm|cann|cana|cang|canl|asaa|asal|asar)");
    public final Pattern RAWS = Pattern.compile("(canp)");
    
    private final int port;
    private final int sessionId;
    
    public DacpService(int port, int sessionId) {
        this.port = port;
        this.sessionId = sessionId;
    }
    
    public void login() throws IOException {
        Response dacpRsp = this.request(new RequestBuilder("/login")
                .addParam("pairing-guid", "0x1")
                .addParam("request-session-id", sessionId));
        System.out.println(dacpRsp);
        if ("Could not start session".equals(dacpRsp.getNested("mlog").getString("msts"))) {
            this.logout();
            this.login();
        }
    }
    
    public Response logout() throws IOException {
        Response dacpRsp = this.request(new RequestBuilder("/logout")
                .withSessionId());
        return dacpRsp;
    }
    
    public List<Speaker> getSpeakers() throws IOException {
        Response dacpRsp = this.request(new RequestBuilder("/ctrl-int/1/getspeakers").withSessionId());
        List<Speaker> result = new ArrayList<>();
        for (Response r : dacpRsp.getNested("casp").findArray("mdcl")) {
            Speaker speaker = new Speaker();
            speaker.setName(r.getString("caia"));
            speaker.setId("0x" + r.getNumberHex("msma"));
            speaker.setVolumePct(r.getNumberLong("cmvo") / 100.0);
            speaker.setActive(r.containsKey("caia") && r.getNumberLong("caia") == 1L);
            result.add(speaker);
        }
        return result;
    }
    
    public Response setActiveSpeakers(List<String> ids) throws IOException {
        Response dacpRsp = this.request(new RequestBuilder("/ctrl-int/1/getspeakers")
                .addParam("speaker-id", ids.stream().collect(Collectors.joining(",")))
                .withSessionId());
        return dacpRsp;
    }
    
    
    public double getMasterVolume() throws IOException {
        Response dacpRsp = this.request(new RequestBuilder("/ctrl-int/1/getproperty")
                .addParam("properties", "dmcp.volume")
                .withSessionId());
        return dacpRsp.getNumberLong("cmvo") / 100.0;
    }
    
    public Response setMasterVolume(double volume) throws IOException {
        return null;
    }
    
    public void setSpeakerVolume(String id, double volume) throws IOException {
        Response dacpRsp = this.request(new RequestBuilder("/ctrl-int/1/getproperty")
                .addParam("properties", (int) (volume * 100))
                .withSessionId());
    }
    
    /*
     * Make a request to daap server and return parsed Response object. Open session if necessary
     */
    private Response request(RequestBuilder builder) throws IOException {
        URLConnection conn = builder.getUrl().openConnection();
        Response rsp = this.performParse(conn.getInputStream());
        
        // TODO check to see if the request was rejected due to lack of session
        // if so, open the connection
        return rsp;
    }
    
    /*
     * Helper class to build requests. Use fluent API to add parameters
     */
    public class RequestBuilder {
        private String url;
        private List<Pair> params;
        
        public RequestBuilder(String url) {
            this.url = url;
            this.params = new ArrayList<>();
        }
        
        public RequestBuilder withSessionId() {
            this.addParam("session-id", sessionId);
            return this;
        }

        public RequestBuilder addParam(String key, Object value) {
            this.params.add(new Pair(key, value));
            return this;
        }

        public URL getUrl() throws MalformedURLException {
            StringBuilder builder = new StringBuilder("http://localhost:")
                .append(port)
                .append(url)
                .append("?")
                .append(this.params.stream()
                        .map(p -> p.getKey() + "=" + p.getValue())
                        .collect(Collectors.joining("&")));
            return new URL(builder.toString());
        }
    }
    
    /*
     * Parses input stream from http response
     * Credit: ResponseParser in https://github.com/nglass/tunesremote-se
     */
    private Response performParse(InputStream is) throws IOException {
        final DataInputStream stream = new DataInputStream(is);
        return this.parse(stream, stream.available());
    }

    /*
     * Recursive parse helper
     * Credit: ResponseParser in https://github.com/nglass/tunesremote-se
     */
    private Response parse(DataInputStream raw, int handle) throws IOException {
        final Response resp = new Response();
        int progress = 0;

        // loop until done with the section we've been assigned
        while (handle > 0) {
            final String key = this.readString(raw, 4);
            final int length = raw.readInt();
            handle -= 8 + length;
            progress += 8 + length;

            // handle key collisions by using index notation
            final String nicekey = resp.containsKey(key) ? String.format("%s[%06d]", key, progress) : key;

            if (BRANCHES.matcher(key).matches()) {
                // recurse off to handle branches
                final Response branch = this.parse(raw, length);
                resp.put(nicekey, branch);

            } else if (STRINGS.matcher(key).matches()) {
                // force handling as string
                resp.put(nicekey, this.readString(raw, length));
            } else if (RAWS.matcher(key).matches()) {
                // force handling as raw
                resp.put(nicekey, this.readRaw(raw, length));
            } else if (length == 1 || length == 2 || length == 4 || length == 8) {
                // handle parsing unsigned bytes, ints, longs
                resp.put(nicekey, new BigInteger(1, this.readRaw(raw, length)));
            } else {
                // fallback to just parsing as string
                resp.put(nicekey, this.readString(raw, length));
            }
        }
        return resp;
    }

    /*
     * Parse helper
     * Credit: ResponseParser in https://github.com/nglass/tunesremote-se
     */
    private byte[] readRaw(DataInputStream raw, int length) throws IOException {
        byte[] buf = new byte[length];
        raw.read(buf, 0, length);
        return buf;
    }

    /*
     * Parse helper
     * Credit: ResponseParser in https://github.com/nglass/tunesremote-se
     */
    private String readString(DataInputStream raw, int length) throws IOException {
        byte[] key = new byte[length];
        raw.read(key, 0, length);
        return new String(key);
    }
    
    /*
     * Stores an attribute pair for building requests (see RequestBuilder)
     */
    private class Pair {
        private final String key;
        private final Object value;
        public Pair(String key, Object value) {
            super();
            this.key = key;
            this.value = value;
        }
        public String getKey() {
            return key;
        }
        public Object getValue() {
            return value;
        }
    }
    
    private class Response extends HashMap<String, Object> {

        public Response getNested(String key) {
            return (Response) this.get(key);
        }

        public String getString(String key) {
            Object obj = this.get(key);
            if (obj instanceof String)
                return (String) obj;
            else
                return "";
        }

        public BigInteger getNumber(String key) {
            Object obj = this.get(key);
            if (obj instanceof BigInteger)
                return (BigInteger) obj;
            else
                return new BigInteger("-1");
        }

        public long getNumberLong(String key) {
            return getNumber(key).longValue();
        }

        public String getNumberString(String key) {
            return getNumber(key).toString();
        }

        public String getNumberHex(String key) {
            return Long.toHexString(getNumberLong(key));
        }

        public byte[] getRaw(String key) {
            Object obj = this.get(key);
            return (byte[]) obj;
        }

        public List<Response> findArray(String prefix) {
            List<Response> found = new LinkedList<Response>();

            // find all values with same key prefix
            // sort keys to make sure we return in original order

            String[] keys = this.keySet().toArray(new String[] {});
            Arrays.sort(keys);

            for (String key : keys) {
                if (key.startsWith(prefix))
                    found.add((Response) this.get(key));
            }

            return found;
        }
    }
    
    public class Speaker {
        
        private String name;
        private String id;
        private boolean active;
        private double volumePct;

        public Speaker() {}
        
        public Speaker(String name, boolean active, double volumePct) {
            super();
            this.name = name;
            this.active = active;
            this.volumePct = volumePct;
        }
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public String getId() {
            return id;
        }
        public void setId(String id) {
            this.id = id;
        }
        public boolean isActive() {
            return active;
        }
        public void setActive(boolean active) {
            this.active = active;
        }
        public double getVolumePct() {
            return volumePct;
        }
        public void setVolumePct(double volumePct) {
            this.volumePct = volumePct;
        }

        @Override
        public String toString() {
            return "Speaker [name=" + name + ", id=" + id + ", active=" + active
                    + ", volumePct=" + volumePct + "]";
        }
    }

//    public static void main(final String[] args) throws Exception {
//        DacpService dacp = new DacpService(3689, 51);
//        dacp.login();
//        dacp.getMasterVolume();
//        List<Speaker> speakers = dacp.getSpeakers();
//        dacp.logout();
//    }
}