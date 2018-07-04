import java.util.ArrayList;

/**
 * Created by Catz on 4/12/14.
 *
 * Wrapper class for managing a collection of some object.
 */
class ObjectManager<T> {
    private ArrayList<T> all;

    ObjectManager(){
        all = new ArrayList<T>();
    }

    void add(T object){
        all.add(object);
    }

    void clearAll(){
        all.clear();
    }

    ArrayList<T> getAll(){
        return all;
    }

    int getNumber(){
        return all.size();
    }
}
