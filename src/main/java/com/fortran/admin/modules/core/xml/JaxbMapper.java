package com.fortran.admin.modules.core.xml;

import com.fortran.admin.modules.core.utils.ReflectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.util.Assert;

import javax.xml.bind.*;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.namespace.QName;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 使用Jaxb2.0实现XML<->Java Object的Mapper.
 * <p>
 * 在创建时需要设定所有需要序列化的Root对象的Class.
 * 特别支持Root对象是Collection的情形.
 *
 * @author calvin
 * @version 2013-01-15
 */
@SuppressWarnings("rawtypes")
@Slf4j
public class JaxbMapper {

    private static ConcurrentMap<Class, JAXBContext> jaxbContexts = new ConcurrentHashMap<Class, JAXBContext>();

    /**
     * Java Object->Xml without encoding.
     */
    public static String toXml(Object root) {
        Class clazz = ReflectionUtils.getUserClass(root);
        return toXml(root, clazz, null);
    }

    /**
     * Java Object->Xml with encoding.
     */
    public static String toXml(Object root, String encoding) {
        Class clazz = ReflectionUtils.getUserClass(root);
        return toXml(root, clazz, encoding);
    }

    /**
     * Java Object->Xml with encoding.
     */
    public static String toXml(Object root, Class clazz, String encoding) {
        StringWriter writer = new StringWriter();
        try {
            createMarshaller(clazz, encoding).marshal(root, writer);
        } catch (JAXBException e) {
            log.error(" to xml error.", e);
        }
        return writer.toString();
    }

    /**
     * Java Collection->Xml without encoding, 特别支持Root Element是Collection的情形.
     */
    public static String toXml(Collection<?> root, String rootName, Class clazz) {
        return toXml(root, rootName, clazz, null);
    }

    /**
     * Java Collection->Xml with encoding, 特别支持Root Element是Collection的情形.
     */
    public static String toXml(Collection<?> root, String rootName, Class clazz, String encoding) {
        StringWriter writer = new StringWriter();
        try {
            CollectionWrapper wrapper = new CollectionWrapper();
            wrapper.collection = root;
            JAXBElement<CollectionWrapper> wrapperElement = new JAXBElement<CollectionWrapper>(new QName(rootName),
                    CollectionWrapper.class, wrapper);
            createMarshaller(clazz, encoding).marshal(wrapperElement, writer);
        } catch (JAXBException e) {
            log.error(" to xml error.", e);
        }
        return writer.toString();
    }

    /**
     * Xml->Java Object.
     */
    @SuppressWarnings("unchecked")
    public static <T> T fromXml(String xml, Class<T> clazz) throws JAXBException {
        StringReader reader = null;
        try {
            reader = new StringReader(xml);
            return (T) createUnmarshaller(clazz).unmarshal(reader);
        } catch (JAXBException e) {
            throw e;
        }

    }

    /**
     * 创建Marshaller并设定encoding(可为null).
     * 线程不安全，需要每次创建或pooling。
     */
    public static Marshaller createMarshaller(Class clazz, String encoding) throws JAXBException {
        try {
            JAXBContext jaxbContext = getJaxbContext(clazz);

            Marshaller marshaller = jaxbContext.createMarshaller();

            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            if (StringUtils.isNotBlank(encoding)) {
                marshaller.setProperty(Marshaller.JAXB_ENCODING, encoding);
            }

            return marshaller;
        } catch (JAXBException e) {
            throw e;
        }
    }

    /**
     * 创建UnMarshaller.
     * 线程不安全，需要每次创建或pooling。
     */
    public static Unmarshaller createUnmarshaller(Class clazz) throws JAXBException {
        try {
            JAXBContext jaxbContext = getJaxbContext(clazz);
            return jaxbContext.createUnmarshaller();
        } catch (JAXBException e) {
            throw e;
        }
    }

    protected static JAXBContext getJaxbContext(Class clazz) {
        Assert.notNull(clazz, "'clazz' must not be null");
        JAXBContext jaxbContext = jaxbContexts.get(clazz);
        if (jaxbContext == null) {
            try {
                jaxbContext = JAXBContext.newInstance(clazz, CollectionWrapper.class);
                jaxbContexts.putIfAbsent(clazz, jaxbContext);
            } catch (JAXBException ex) {
                throw new HttpMessageConversionException("Could not instantiate JAXBContext for class [" + clazz
                        + "]: " + ex.getMessage(), ex);
            }
        }
        return jaxbContext;
    }

    /**
     * 封装Root Element 是 Collection的情况.
     */
    public static class CollectionWrapper {
        @XmlAnyElement
        protected Collection<?> collection;
    }

}