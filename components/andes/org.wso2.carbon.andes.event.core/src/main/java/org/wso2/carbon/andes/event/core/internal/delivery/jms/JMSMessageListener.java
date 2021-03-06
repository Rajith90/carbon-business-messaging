/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.andes.event.core.internal.delivery.jms;

import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.andes.event.core.NotificationManager;
import org.wso2.carbon.andes.event.core.exception.EventBrokerException;
import org.wso2.carbon.andes.event.core.subscription.Subscription;
import org.wso2.carbon.context.PrivilegedCarbonContext;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.util.Enumeration;

/**
 * JMS message listener class which trigger based on message receive
 */
public class JMSMessageListener implements MessageListener {

    private Log log = LogFactory.getLog(JMSMessageListener.class);
    private NotificationManager notificationManager;
    private Subscription subscription;

    /**
     * JMS message listener class constructor which initialize notification manager and subscription
     * @param notificationManager
     * @param subscription
     */
    public JMSMessageListener(NotificationManager notificationManager, Subscription subscription) {
        this.notificationManager = notificationManager;
        this.subscription = subscription;
    }

    /**
     * Renew subscription object
     *
     * @param subscription subscription
     */
    public void renewSubscription(Subscription subscription){
        this.subscription.setExpires(subscription.getExpires());
        this.subscription.setProperties(subscription.getProperties());
    }

    /**
     * Fire when message receive and send notification to carbon notification manager
     *
     * @param message message
     */
    public void onMessage(Message message) {
        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(this.subscription.getTenantId());
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername(this.subscription.getOwner());
            PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain(true);
            if (message instanceof TextMessage) {
                TextMessage textMessage = (TextMessage) message;
                org.wso2.carbon.andes.event.core.Message messageToSend =
                        new org.wso2.carbon.andes.event.core.Message();
                messageToSend.setMessage(textMessage.getText());
                // set the properties
                Enumeration propertyNames = message.getPropertyNames();
                String key = null;
                while (propertyNames.hasMoreElements()){
                    key = (String) propertyNames.nextElement();
                    messageToSend.addProperty(key, message.getStringProperty(key));
                }

                this.notificationManager.sendNotification(messageToSend, this.subscription);
            } else {
                log.warn("Non text message received");
            }
        } catch (JMSException e) {
            log.error("Can not read the text message ", e);
        } catch (EventBrokerException e) {
            log.error("Can not send the notification ", e);
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }

    }
}
