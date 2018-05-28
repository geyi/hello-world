package com.zpush;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class NotificationBuilder {
    static final Logger logger = LoggerFactory.getLogger(NotificationBuilder.class);
    byte[] token = null;
    int priority = 10;
    private Date expirationDate = new Date(92464560000000L);
    JSONObject payload = new JSONObject();
    JSONObject aps = new JSONObject();
    String alert = null;
    JSONObject alertObject = new JSONObject();
    final HashMap<String, Object> customProperties = new HashMap();
    static final Charset utf8 = Charset.forName("utf-8");
    static final int MAX_PAYLOAD_SIZE_8 = 256;
    static final int MAX_PAYLOAD_SIZE_8_9 = 2048;
    static final int MAX_PAYLOAD_SIZE_9 = 4096;
    private int MAX_PAYLOAD_SIZE = 0;
    private String IOS_VERSION = null;

    public NotificationBuilder setIosVersion(String version) {
        if (version == null) {
            version = "8.0.0";
        }

        this.IOS_VERSION = version;
        Integer ver = 8;

        try {
            ver = Integer.parseInt(version.substring(0, version.indexOf(".")));
        } catch (Exception var4) {
            logger.error(var4.getMessage(), var4);
        }

        if (ver < 8) {
            this.MAX_PAYLOAD_SIZE = 256;
        } else if (ver == 8) {
            this.MAX_PAYLOAD_SIZE = 2048;
        } else if (ver >= 9) {
            this.MAX_PAYLOAD_SIZE = 4096;
        }

        return this;
    }

    public Notification build(String payload) {
        if (payload == null) {
            throw new IllegalArgumentException("payload is null!");
        } else {
            Notification notification = new Notification();
            if (this.token != null) {
                notification.setToken(this.token);
                if (this.IOS_VERSION == null) {
                    this.setIosVersion((String) null);
                }

                notification.setPriority(this.priority);
                notification.setExpirationDate(this.expirationDate);
                byte[] bytes = payload.toString().getBytes(utf8);
                if (bytes.length > this.MAX_PAYLOAD_SIZE) {
                    throw new IllegalArgumentException("payload.length >" + this.MAX_PAYLOAD_SIZE);
                } else {
                    notification.setPayload(bytes);
                    return notification;
                }
            } else {
                throw new IllegalArgumentException("token is null!");
            }
        }
    }

    public Notification build() {
        Notification notification = new Notification();
        if (this.token != null) {
            notification.setToken(this.token);
            if (this.IOS_VERSION == null) {
                this.setIosVersion((String) null);
            }

            notification.setPriority(this.priority);
            notification.setExpirationDate(this.expirationDate);
            if (this.alert != null) {
                this.aps.put("alert", this.alert);
            } else {
                this.aps.put("alert", this.alertObject);
            }

            if (this.alert != null && !this.alertObject.isEmpty()) {
                logger.warn("can not set alert and alertObject both!, https://developer.apple" +
                        ".com/library/ios/documentation/NetworkingInternet/Conceptual/RemoteNotificationsPG/Chapters" +
                        "/ApplePushService.html#//apple_ref/doc/uid/TP40008194-CH100-SW12");
            }

            this.payload.put("aps", this.aps);
            byte[] bytes = this.payload.toString().getBytes(utf8);
            if (bytes.length > this.MAX_PAYLOAD_SIZE) {
                throw new IllegalArgumentException("payload.length >" + this.MAX_PAYLOAD_SIZE);
            } else {
                notification.setPayload(bytes);
                return notification;
            }
        } else {
            throw new IllegalArgumentException("token is null!");
        }
    }

    public NotificationBuilder setToken(byte[] token) {
        if (token.length != 32) {
            throw new IllegalArgumentException("token.length != 32");
        } else {
            this.token = token;
            return this;
        }
    }

    public NotificationBuilder setToken(String token) {
        try {
            byte[] hex = Hex.decodeHex(token.toCharArray());
            this.setToken(hex);
            return this;
        } catch (DecoderException var3) {
            throw new RuntimeException(var3);
        }
    }

    public NotificationBuilder setPriority(int priority) {
        this.priority = priority;
        return this;
    }

    public NotificationBuilder setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
        return this;
    }

    public NotificationBuilder setBadge(int badge) {
        this.aps.put("badge", badge);
        return this;
    }

    public NotificationBuilder setSound(String sound) {
        this.aps.put("sound", sound);
        return this;
    }

    public NotificationBuilder setcontentAvailable(boolean contentAvilable) {
        this.aps.put("content-available", 1);
        return this;
    }

    public NotificationBuilder setAlert(String alert) {
        this.alert = alert;
        return this;
    }

    public NotificationBuilder setAlertBody(String alertBody) {
        this.alertObject.put("body", alertBody);
        return this;
    }

    public NotificationBuilder setAlertActionLocKey(String alertActionLocKey) {
        this.alertObject.put("action-loc-key", alertActionLocKey);
        return this;
    }

    public NotificationBuilder setAlertLocKey(String alertLocKey) {
        this.alertObject.put("loc-key", alertLocKey);
        return this;
    }

    public NotificationBuilder setAlertLocArgs(String... alertLocArgs) {
        this.alertObject.put("loc-args", alertLocArgs);
        return this;
    }

    public NotificationBuilder setAlertLocArgs(List<String> alertLocArgs) {
        this.alertObject.put("loc-args", alertLocArgs);
        return this;
    }

    public NotificationBuilder setAlertLunchImage(String alertLunchImage) {
        this.alertObject.put("launch-image", alertLunchImage);
        return this;
    }

    public NotificationBuilder setUserProperty(String key, Object value) {
        this.payload.put(key, value);
        return this;
    }
}