package net.nologin.meep.pingly.model;


public enum ProbeType implements Comparable<ProbeType> {

    SocketConnection(0),
    HTTPResponse(1),
    HeaderPresent(2),
    HeaderValue(3),
    ResponseSize(4),
    ResponsebodyContents(5),
    PinglyJSON(6);

    public int id;
    
    
    ProbeType(int id){
        this.id = id;    
    }

    
    public String getResourceNameForName(){
        return "probe_type_" + id + "_name";
    }

    
    public String getResourceNameForDesc(){
        return "probe_type_" + id + "_desc";
    }


    public static ProbeType fromId(int id){
        for(ProbeType t : ProbeType.values()){
            if(id == t.id){
                return t;
            }
        }
        throw new IllegalArgumentException("ID " + id  + " not a valid " + ProbeType.class.getSimpleName());
    }

    @Override
    public String toString(){
        return super.toString() + "[" + id + "]";
    }
}
