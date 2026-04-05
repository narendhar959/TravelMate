package com.klu.models;

import java.util.Map;

public class ChatResponse {
    public String              intent;
    public String              reply;
    public Map<String, Object> booking_state;
    public boolean             booking_ready;
    public boolean             booking_created;
    public boolean             quote_ready;
    public boolean             payment_required;
    public boolean             chat_closed;
    public boolean             saved_locally;
    public Double              quote_amount;
    public String              booking_summary;
    public Map<String, Object> booking;
    public String              error;
}
