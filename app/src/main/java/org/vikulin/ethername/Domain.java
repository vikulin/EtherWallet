package org.vikulin.ethername;

import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.TXTRecord;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import java.net.UnknownHostException;

public class Domain {

    public static class Status {

        public Status (Boolean isAvailable, String resolved, int result){
            this.isAvailable = isAvailable;
            this.resolved = resolved;
            this.result = result;
        }
        private Boolean isAvailable;
        private String resolved;
        private int result;

        public Boolean isAvailable(){
            return isAvailable;
        }

        public String getResolved(){
            return resolved;
        }

        public int getResult(){
            return result;
        }

        @Override
        public String toString() {
            return "isAvailable:"+isAvailable+" resolved:"+resolved+" result:"+result;
        }
    }

    public static final String DNS = "ns1.hyperborian.org";

    public static Status isAvailable(String domain) throws TextParseException, UnknownHostException {
        Lookup l = new Lookup(domain, Type.TXT);
        SimpleResolver sr = new SimpleResolver(DNS);
        l.setResolver(sr);
        //sr.setTCP(true);
        Record [] records = l.run();
        if(l.getResult()== Lookup.SUCCESSFUL){
            if(records!=null) {
                return new Status(Boolean.FALSE, records[0].rdataToString().replaceAll("\"", ""), l.getResult());
            } else {
                throw new TextParseException("Response was successful but no records returned");
            }
        } else {
            if(l.getResult()== Lookup.HOST_NOT_FOUND) {
                return new Status(Boolean.TRUE, null, l.getResult());
            } else {
                return new Status(null, null, l.getResult());
            }
        }
    }

    public static Status resolve(String domain) throws TextParseException, UnknownHostException{
        Lookup l = new Lookup(domain, Type.TXT);
        SimpleResolver sr = new SimpleResolver(DNS);
        l.setResolver(sr);
        //sr.setTCP(true);
        sr.setIgnoreTruncation(false);
        Record [] records = l.run();
        if(l.getResult()== Lookup.SUCCESSFUL){
            return new Status(Boolean.FALSE, records[0].rdataToString().replaceAll("\"",""), l.getResult());
        } else {
            return new Status(null, null, l.getResult());
        }
    }


    public static void main( String[] args ) throws TextParseException, UnknownHostException {
        Domain.isAvailable("vikulin.eth");
        String value = "vikulin.eth";
        System.out.println(Domain.resolve(value));
        System.out.println(Domain.resolve(value).getResolved());
    }
}
