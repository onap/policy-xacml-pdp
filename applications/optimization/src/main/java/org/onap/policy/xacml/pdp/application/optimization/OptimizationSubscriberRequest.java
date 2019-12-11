package org.onap.policy.xacml.pdp.application.optimization;

import com.att.research.xacml.api.AttributeValue;
import com.att.research.xacml.api.DataType;
import com.att.research.xacml.api.DataTypeException;
import com.att.research.xacml.api.DataTypeFactory;
import com.att.research.xacml.api.Identifier;
import com.att.research.xacml.api.Request;
import com.att.research.xacml.api.XACML3;
import com.att.research.xacml.std.StdMutableAttribute;
import com.att.research.xacml.std.StdMutableRequest;
import com.att.research.xacml.std.StdMutableRequestAttributes;
import com.att.research.xacml.std.annotations.XACMLSubject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.pdp.xacml.application.common.ToscaDictionary;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationException;
import org.onap.policy.pdp.xacml.application.common.std.StdMatchablePolicyRequest;

public class OptimizationSubscriberRequest extends StdMatchablePolicyRequest {

    @XACMLSubject(attributeId = "urn:org:onap:optimization:subscriber:name", includeInResults = true)
    List<String> subscriberRoles;

    /**
     * Create an instance of xacml request.
     *
     * @param decisionRequest Incoming DecisionRequest object
     * @return XACML request
     * @throws XacmlApplicationException XacmlApplicationException
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Request createInstance(DecisionRequest decisionRequest) throws XacmlApplicationException {
        Request request = StdMatchablePolicyRequest.createInstance(decisionRequest);

        //
        // Add in the subject attributes
        //
        StdMutableRequest mutableRequest = new StdMutableRequest(request);
        StdMutableRequestAttributes subjectAttributes = new StdMutableRequestAttributes();
        subjectAttributes.setCategory(XACML3.ID_SUBJECT_CATEGORY_ACCESS_SUBJECT);
        //
        // Add the subject attributes
        //
        Map<String, Object> subjects = decisionRequest.getSubject();
        for (Entry<String, Object> entrySet : subjects.entrySet()) {
            try {
                if (entrySet.getValue() instanceof Collection) {
                    addSubject(subjectAttributes, (Collection) entrySet.getValue(),
                            ToscaDictionary.ID_SUBJECT_OPTIMIZATION_SUBSCRIBER_NAME);
                } else {
                    addSubject(subjectAttributes, Arrays.asList(entrySet.getValue().toString()),
                            ToscaDictionary.ID_SUBJECT_OPTIMIZATION_SUBSCRIBER_NAME);
                }
            } catch (DataTypeException e) {
                throw new XacmlApplicationException("Failed to add resource ", e);
            }
        }
        mutableRequest.add(subjectAttributes);
        return mutableRequest;
    }

    protected static StdMutableRequestAttributes addSubject(StdMutableRequestAttributes attributes,
            Collection<Object> values, Identifier id) throws DataTypeException {

        DataTypeFactory factory = getDataTypeFactory();
        if (factory == null) {
            return null;
        }
        for (Object value : values) {
            StdMutableAttribute mutableAttribute    = new StdMutableAttribute();
            mutableAttribute.setCategory(XACML3.ID_SUBJECT_CATEGORY_ACCESS_SUBJECT);
            mutableAttribute.setAttributeId(id);
            mutableAttribute.setIncludeInResults(true);

            DataType<?> dataTypeExtended    = factory.getDataType(XACML3.ID_DATATYPE_STRING);
            AttributeValue<?> attributeValue = dataTypeExtended.createAttributeValue(value);
            Collection<AttributeValue<?>> attributeValues = new ArrayList<>();
            attributeValues.add(attributeValue);
            mutableAttribute.setValues(attributeValues);

            attributes.add(mutableAttribute);
        }
        return attributes;
    }
}
