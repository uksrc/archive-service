package org.uksrc.archive.utils;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;
import jakarta.xml.bind.*;
import org.ivoa.dm.caom2.caom2.DerivedObservation;
import org.ivoa.dm.caom2.caom2.Observation;
import org.ivoa.dm.caom2.caom2.SimpleObservation;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Custom "interceptor" for Observation messages to assist with the instantiation of Observation derived classes.
 * As Observation is an abstract class, JAXB requires assistance determining which subtype to instantiate.
 * Called when an Observation (subtype typically) is posted to one of the endpoints @see ObservationResource.
 */
@Provider
@Consumes(MediaType.APPLICATION_XML)
public class ObservationMessageBodyReader implements MessageBodyReader<Observation> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return Observation.class.isAssignableFrom(type);
    }

    @Override
    public Observation readFrom(Class<Observation> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                                MultivaluedMap<String, String> httpHeaders, InputStream entityStream) {
        try {
            JAXBContext context = JAXBContext.newInstance(Observation.class, SimpleObservation.class, DerivedObservation.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();

            Object result = unmarshaller.unmarshal(entityStream);
            if (result instanceof JAXBElement<?> element) {
                if (element.getValue() instanceof Observation) {
                    return (Observation) element.getValue();
                }
            }
            throw new BadRequestException("Invalid XML: Unable to parse Observation object");
        } catch (JAXBException e) {
            throw new WebApplicationException("Error deserializing XML", e, Response.Status.BAD_REQUEST);
        }
    }
}

