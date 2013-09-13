package com.totsp.keying.dao;

import com.totsp.keying.definition.KeySegment;
import com.totsp.keying.impl.Component;
import com.totsp.keying.impl.Generator;
import com.totsp.keying.impl.NonDeterministicComponent;
import com.totsp.keying.impl.PropertyComponent;
import com.totsp.keying.impl.TimeComponent;
import com.totsp.keying.impl.UUIDComponent;
import com.totsp.keying.reflect.KeyException;
import com.totsp.keying.reflect.Reader;
import com.totsp.keying.reflect.Setter;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class KeyGenerator {

    private static final Map<Class, Generator> GENERATORS = new ConcurrentHashMap<Class, Generator>();

    private static <T> Generator<T> get(T o){
        Generator<T> generator = GENERATORS.get(o.getClass());
        if(generator == null){
            Setter<T> t = new Setter<T>((Class<T>) o.getClass());
            ArrayList<Component<T>> components = new ArrayList<Component<T>>(t.strategy.value().length);
            int propertyIndex = 0;
            Class<T> type = (Class<T>) o.getClass();
            for(KeySegment segment : t.strategy.value()){
                switch(segment){
                    case PROPERTY:
                        if(t.strategy.properties().length -1 > propertyIndex){
                            throw new KeyException("Expected "+propertyIndex+" properties but found only "+propertyIndex);
                        }
                        components.add(new PropertyComponent<T>(new Reader<T>(type, t.strategy.properties()[propertyIndex])));
                        propertyIndex++;
                        if(t.strategy.properties().length -1 > propertyIndex){
                            throw new KeyException("Excented "+propertyIndex+" properties but found an extra "+(t.strategy.properties().length -1 -propertyIndex));
                        }
                        break;
                    case UUID:
                        components.add(new UUIDComponent<T>());
                        break;
                    case TIME:
                        components.add(new TimeComponent<T>(false));
                        if(t.strategy.value().length == 1){
                            throw new KeyException(o.getClass().getCanonicalName()+" cannot a a key value of only a time.");
                        }
                        break;
                    case INVERSE_TIME:
                        components.add(new TimeComponent<T>(true));
                        if(t.strategy.value().length == 1){
                            throw new KeyException(o.getClass().getCanonicalName()+" cannot a a key value of only a time.");
                        }
                        break;
                    default:
                        throw new KeyException("Unknown segment type "+segment);
                }
            }
            generator = new Generator<T>(components.toArray(new Component[components.size()]), t);
            GENERATORS.put(o.getClass(), generator);
        }
        return generator;
    }

    public static <T> T key(T o){
        Generator<T> gen = get(o);
        if(!gen.keyed(o)){
            get(o).key(o);
        }
        get(o).key(o);
        return o;
    }

    public static <T> String compute(T o){
       Generator<T> generator = get(o);
       for(Component<T> component: generator.components){
           if(component instanceof NonDeterministicComponent){
            throw new KeyException(component.getClass().getCanonicalName() +" isn't a deterministic component.");
           }
       }
       return generator.compute(o);
    }
}