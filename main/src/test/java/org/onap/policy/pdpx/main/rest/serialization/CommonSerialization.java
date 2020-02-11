package org.onap.policy.pdpx.main.rest.serialization;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.att.research.xacml.api.Request;
import com.att.research.xacml.api.Response;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.core.MediaType;

public class CommonSerialization {

    @SuppressWarnings("rawtypes")
    private static final Class REQUEST_CLASS = Request.class;
    @SuppressWarnings("rawtypes")
    private static final Class RESPONSE_CLASS = Response.class;
    @SuppressWarnings("rawtypes")
    private static final Class GENERAL_CLASS = MyObject.class;

    public static void testIsWritableOrReadable(String primaryType, String subType,
            MultiArgsFunction<Class<?>, Type, Annotation[], MediaType, Boolean> getter) {

        // null media type
        assertFalse(getter.apply(null, null, null, null));

        // valid media type and class type
        assertTrue("writeable " + subType, getter.apply(
                REQUEST_CLASS, null, null, new MediaType(primaryType, subType)));
        assertTrue("writeable " + subType, getter.apply(
                RESPONSE_CLASS, null, null, new MediaType(primaryType, subType)));

        // valid media type but invalid class type
        assertFalse(getter.apply(
                GENERAL_CLASS, null, null, new MediaType(primaryType, subType)));

        // null subtype or invalid media type
        assertFalse(getter.apply(null, null, null, new MediaType(primaryType, null)));
        assertFalse(getter.apply(null, null, null, MediaType.APPLICATION_JSON_TYPE));
    }

    public static class MyObject {
        private int id;

        public MyObject() {
            super();
        }

        public MyObject(int id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return "MyObject [id=" + id + "]";
        }
    }

    @FunctionalInterface
    public interface MultiArgsFunction<T, U, V, W, R> {
        public R apply(T value1, U value2, V value3, W value4);
    }
}