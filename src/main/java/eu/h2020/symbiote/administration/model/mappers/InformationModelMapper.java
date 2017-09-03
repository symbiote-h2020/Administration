package eu.h2020.symbiote.administration.model.mappers;

import eu.h2020.symbiote.core.model.InformationModel;

import java.util.Comparator;

/**
 * Created by vasgl on 9/3/2017.
 */
public class InformationModelMapper implements Comparable<InformationModelMapper>{

    private String id;
    private String name;

    public InformationModelMapper() {
    }

    public InformationModelMapper(String id, String name) {
        setId(id);
        setName(name);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isSelected(String id){
        if (id != null) {
            return this.id.equals(id);
        }
        return false;
    }

    @Override
    public int compareTo(InformationModelMapper o) {
        String compareName = ((InformationModelMapper) o).getName();
        int res = String.CASE_INSENSITIVE_ORDER.compare(getName(), compareName);
        return (res != 0)? res : getName().compareTo(compareName);
    }

    public static Comparator<InformationModelMapper> NameComparator = new Comparator<InformationModelMapper>() {
        @Override
        public int compare(InformationModelMapper o1, InformationModelMapper o2) {
            String name1 = o1.getName();
            String name2 = o2.getName();

            int res = String.CASE_INSENSITIVE_ORDER.compare(name1, name2);
            return (res != 0)? res : name1.compareTo(name2);
        }
    };
}
