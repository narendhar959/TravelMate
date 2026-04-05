package com.klu.service;

import com.klu.entity.Booking;
import com.klu.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class BookingService {

    @Autowired(required=false)
    private BookingRepository repo;

    public Booking createBooking(Booking booking) {
        booking.setBookingRef("TM" + UUID.randomUUID().toString().substring(0,8));
        return repo.save(booking);
    }

    public List<Booking> getBookings(int userId) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId);
    }
}