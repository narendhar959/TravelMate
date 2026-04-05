// Enhanced Booking Modal System for TravelMate
class BookingModal {
  constructor() {
    this.currentStep = 1;
    this.totalSteps = 4;
    this.bookingData = {};
    this.init();
  }

  init() {
    if (!document.getElementById("booking-modal-styles")) {
      const link = document.createElement("link");
      link.id = "booking-modal-styles";
      link.rel = "stylesheet";
      link.href = "/css/booking-modal.css";
      document.head.appendChild(link);
    }
  }

  showBookingModal(serviceData) {
    this.bookingData = { ...serviceData };
    this.currentStep = 1;
    this.createModal();
  }

  createModal() {
    this.closeModal();

    const overlay = document.createElement("div");
    overlay.className = "booking-overlay";
    overlay.id = "booking-overlay";

    const modal = document.createElement("div");
    modal.className = "booking-modal";

    const header = this.createHeader();

    const progressBar = this.createProgressBar();
    const content = this.createStepContent();

    const footer = this.createFooter();

    modal.appendChild(header);
    modal.appendChild(progressBar);
    modal.appendChild(content);
    modal.appendChild(footer);
    overlay.appendChild(modal);

    document.body.appendChild(overlay);

    setTimeout(() => {
      overlay.classList.add("show");
    }, 10);

    overlay.addEventListener("click", (e) => {
      e.stopPropagation();
    });

    const escapeHandler = (e) => {
      if (e.key === "Escape") {
        this.closeModal();
        document.removeEventListener("keydown", escapeHandler);
      }
    };
    document.addEventListener("keydown", escapeHandler);
  }

  createHeader() {
    const header = document.createElement("div");
    header.className = "booking-header";

    const closeBtn = document.createElement("button");
    closeBtn.className = "booking-close";
    closeBtn.innerHTML = "x";
    closeBtn.onclick = () => this.closeModal();

    const title = document.createElement("h2");
    title.className = "booking-title";
    title.textContent = `Complete Your ${
      this.bookingData.type || "Service"
    } Booking`;

    const subtitle = document.createElement("p");
    subtitle.className = "booking-subtitle";
    subtitle.textContent =
      this.bookingData.title || "Complete your booking in a few simple steps";

    header.appendChild(closeBtn);
    header.appendChild(title);
    header.appendChild(subtitle);

    return header;
  }

  createProgressBar() {
    const progressContainer = document.createElement("div");
    progressContainer.className = "progress-container";

    const steps = [
      { number: 1, title: "Service Details", icon: "📋" },
      { number: 2, title: "Passenger Details", icon: "👤" },
      { number: 3, title: "Payment Method", icon: "💳" },
      { number: 4, title: "Confirmation", icon: "✅" },
    ];

    steps.forEach((step, index) => {
      const stepElement = document.createElement("div");
      stepElement.className = `progress-step ${
        this.currentStep >= step.number ? "active" : ""
      } ${this.currentStep > step.number ? "completed" : ""}`;

      stepElement.innerHTML = `
        <div class="step-icon">${step.icon}</div>
        <div class="step-title">${step.title}</div>
        <div class="step-number">${step.number}</div>
      `;

      progressContainer.appendChild(stepElement);

      if (index < steps.length - 1) {
        const connector = document.createElement("div");
        connector.className = `step-connector ${
          this.currentStep > step.number ? "completed" : ""
        }`;
        progressContainer.appendChild(connector);
      }
    });

    return progressContainer;
  }

  createStepContent() {
    const content = document.createElement("div");
    content.className = "booking-content";
    content.id = "booking-content";

    switch (this.currentStep) {
      case 1:
        content.appendChild(this.createServiceDetailsStep());
        break;
      case 2:
        content.appendChild(this.createPassengerDetailsStep());
        break;
      case 3:
        content.appendChild(this.createPaymentStep());
        break;
      case 4:
        content.appendChild(this.createConfirmationStep());
        break;
    }

    return content;
  }

  createServiceDetailsStep() {
    const step = document.createElement("div");
    step.className = "booking-step";

    step.innerHTML = `
      <div class="step-header">
        <h3>📋 Service Details</h3>
        <p>Review your selected service</p>
      </div>
      
      <div class="service-summary">
        <div class="service-card">
          <div class="service-icon">${this.getServiceIcon()}</div>
          <div class="service-info">
            <h4>${this.bookingData.title || "Selected Service"}</h4>
            <div class="service-details">
              ${
                this.bookingData.from && this.bookingData.to
                  ? `
                <div class="detail-row">
                  <span class="label">Route:</span>
                  <span class="value">${this.bookingData.from} → ${this.bookingData.to}</span>
                </div>
              `
                  : ""
              }
              ${
                this.bookingData.date
                  ? `
                <div class="detail-row">
                  <span class="label">Date:</span>
                  <span class="value">${new Date(
                    this.bookingData.date
                  ).toLocaleDateString()}</span>
                </div>
              `
                  : ""
              }
              ${
                this.bookingData.time
                  ? `
                <div class="detail-row">
                  <span class="label">Time:</span>
                  <span class="value">${this.bookingData.time}</span>
                </div>
              `
                  : ""
              }
              <div class="detail-row">
                <span class="label">Price:</span>
                <span class="value price">${
                  this.bookingData.price || "₹0"
                }</span>
              </div>
            </div>
          </div>
        </div>
        
        <div class="additional-options">
          <h4>Additional Options</h4>
          <div class="option-group">
            <label class="checkbox-label">
              <input type="checkbox" id="insurance" name="insurance">
              <span class="checkmark"></span>
              Travel Insurance (+₹199)
            </label>
            <label class="checkbox-label">
              <input type="checkbox" id="priority" name="priority">
              <span class="checkmark"></span>
              Priority Booking (+₹99)
            </label>
            ${
              this.bookingData.type === "flight"
                ? `
              <label class="checkbox-label">
                <input type="checkbox" id="meal" name="meal">
                <span class="checkmark"></span>
                Meal Preference (+₹299)
              </label>
              <label class="checkbox-label">
                <input type="checkbox" id="baggage" name="baggage">
                <span class="checkmark"></span>
                Extra Baggage (+₹499)
              </label>
            `
                : ""
            }
          </div>
        </div>
      </div>
    `;

    return step;
  }

  createPassengerDetailsStep() {
    const step = document.createElement("div");
    step.className = "booking-step";

    const totalTravelers = parseInt(this.bookingData.travelers) || 1;

    // Build one form section per traveler
    let passengerSections = "";
    for (let i = 1; i <= totalTravelers; i++) {
      const suffix = i; // used as id suffix: firstName_1, firstName_2, etc.
      const isPrimary = i === 1;
      const label = isPrimary
        ? `Passenger 1 (Primary)`
        : `Passenger ${i}`;

      passengerSections += `
        <div class="form-section" style="margin-bottom:20px;">
          <h4>${label}</h4>
          <div class="form-grid">
            <div class="form-group">
              <label for="firstName_${suffix}">First Name *</label>
              <input type="text" id="firstName_${suffix}" name="firstName_${suffix}" required>
            </div>
            <div class="form-group">
              <label for="lastName_${suffix}">Last Name *</label>
              <input type="text" id="lastName_${suffix}" name="lastName_${suffix}" required>
            </div>
            ${isPrimary ? `
            <div class="form-group">
              <label for="email_${suffix}">Email *</label>
              <input type="email" id="email_${suffix}" name="email_${suffix}" required>
            </div>
            <div class="form-group">
              <label for="phone_${suffix}">Phone Number *</label>
              <input type="tel" id="phone_${suffix}" name="phone_${suffix}" required>
            </div>
            ` : ""}
            <div class="form-group">
              <label for="age_${suffix}">Age *</label>
              <input type="number" id="age_${suffix}" name="age_${suffix}" min="1" max="120" required>
            </div>
            <div class="form-group">
              <label for="gender_${suffix}">Gender *</label>
              <select id="gender_${suffix}" name="gender_${suffix}" required>
                <option value="">Select Gender</option>
                <option value="male">Male</option>
                <option value="female">Female</option>
                <option value="other">Other</option>
              </select>
            </div>
          </div>
          ${
            this.bookingData.type === "flight"
              ? `
            <div class="form-grid" style="margin-top:15px;">
              <div class="form-group">
                <label for="documentType_${suffix}">Document Type *</label>
                <select id="documentType_${suffix}" name="documentType_${suffix}" required>
                  <option value="">Select Document</option>
                  <option value="passport">Passport</option>
                  <option value="aadhar">Aadhar Card</option>
                  <option value="voter">Voter ID</option>
                  <option value="driving">Driving License</option>
                </select>
              </div>
              <div class="form-group">
                <label for="documentNumber_${suffix}">Document Number *</label>
                <input type="text" id="documentNumber_${suffix}" name="documentNumber_${suffix}" required>
              </div>
            </div>
          `
              : ""
          }
        </div>
      `;
    }

    step.innerHTML = `
      <div class="step-header">
        <h3>👤 Passenger Details</h3>
        <p>Enter details for all ${totalTravelers} passenger${totalTravelers > 1 ? "s" : ""}</p>
      </div>
      
      <div class="passenger-form">
        ${passengerSections}

        <div class="form-section">
          <h4>Emergency Contact</h4>
          <div class="form-grid">
            <div class="form-group">
              <label for="emergencyName">Contact Name</label>
              <input type="text" id="emergencyName" name="emergencyName">
            </div>
            <div class="form-group">
              <label for="emergencyPhone">Contact Phone</label>
              <input type="tel" id="emergencyPhone" name="emergencyPhone">
            </div>
          </div>
        </div>
      </div>
    `;

    // Pre-fill primary passenger from logged-in user
    const user = JSON.parse(
      localStorage.getItem("travelmate_current_user") || "null"
    );
    if (user) {
      setTimeout(() => {
        const fn = document.getElementById("firstName_1");
        const ln = document.getElementById("lastName_1");
        const em = document.getElementById("email_1");
        const ph = document.getElementById("phone_1");
        if (fn) fn.value = user.firstname || "";
        if (ln) ln.value = user.lastname || "";
        if (em) em.value = user.email || "";
        if (ph) ph.value = user.phone || "";
      }, 100);
    }

    return step;
  }

  createPaymentStep() {
    const step = document.createElement("div");
    step.className = "booking-step";

    const totalPrice = this.calculateTotalPrice();

    step.innerHTML = `
      <div class="step-header">
        <h3>💳 Payment Method</h3>
        <p>Choose your preferred payment method</p>
      </div>
      
      <div class="payment-section">
        <div class="price-summary">
          <h4>Price Breakdown</h4>
          <div class="price-details">
            <div class="price-row">
              <span>Base Price:</span>
              <span>${this.bookingData.price || "₹0"}</span>
            </div>
            <div class="price-row">
              <span>Taxes & Fees:</span>
              <span>₹${Math.round(totalPrice * 0.1)}</span>
            </div>
            <div class="price-row total">
              <span>Total Amount:</span>
              <span>₹${totalPrice}</span>
            </div>
          </div>
        </div>
        
        <div class="payment-methods">
          <h4>Select Payment Method</h4>
          <div class="payment-options">
            <label class="payment-option">
              <input type="radio" name="paymentMethod" value="card" checked>
              <div class="option-content">
                <div class="option-icon">💳</div>
                <div class="option-text">
                  <strong>Credit/Debit Card</strong>
                  <p>Visa, Mastercard, RuPay</p>
                </div>
              </div>
            </label>
            
            <label class="payment-option">
              <input type="radio" name="paymentMethod" value="upi">
              <div class="option-content">
                <div class="option-icon">📱</div>
                <div class="option-text">
                  <strong>UPI Payment</strong>
                  <p>PhonePe, GPay, Paytm</p>
                </div>
              </div>
            </label>
            
            <label class="payment-option">
              <input type="radio" name="paymentMethod" value="netbanking">
              <div class="option-content">
                <div class="option-icon">🏦</div>
                <div class="option-text">
                  <strong>Net Banking</strong>
                  <p>All major banks</p>
                </div>
              </div>
            </label>
            
            <label class="payment-option">
              <input type="radio" name="paymentMethod" value="wallet">
              <div class="option-content">
                <div class="option-icon">👛</div>
                <div class="option-text">
                  <strong>Digital Wallet</strong>
                  <p>Paytm, Amazon Pay</p>
                </div>
              </div>
            </label>
          </div>
        </div>
        
        <div class="card-details" id="cardDetails">
          <h4>Card Details</h4>
          <div class="form-grid">
            <div class="form-group full-width">
              <label for="cardNumber">Card Number *</label>
              <input type="text" id="cardNumber" name="cardNumber" placeholder="1234 5678 9012 3456" maxlength="19">
            </div>
            <div class="form-group">
              <label for="expiryDate">Expiry Date *</label>
              <input type="text" id="expiryDate" name="expiryDate" placeholder="MM/YY" maxlength="5">
            </div>
            <div class="form-group">
              <label for="cvv">CVV *</label>
              <input type="text" id="cvv" name="cvv" placeholder="123" maxlength="4">
            </div>
            <div class="form-group full-width">
              <label for="cardName">Name on Card *</label>
              <input type="text" id="cardName" name="cardName" placeholder="John Doe">
            </div>
          </div>
        </div>
      </div>
    `;

    setTimeout(() => {
      const paymentOptions = document.querySelectorAll(
        'input[name="paymentMethod"]'
      );
      const cardDetails = document.getElementById("cardDetails");

      paymentOptions.forEach((option) => {
        option.addEventListener("change", () => {
          if (option.value === "card") {
            cardDetails.style.display = "block";
          } else {
            cardDetails.style.display = "none";
          }
        });
      });

      const cardNumberInput = document.getElementById("cardNumber");
      if (cardNumberInput) {
        cardNumberInput.addEventListener("input", (e) => {
          let value = e.target.value.replace(/\s/g, "");
          let formattedValue = value.replace(/(.{4})/g, "$1 ").trim();
          if (formattedValue.length > 19)
            formattedValue = formattedValue.substr(0, 19);
          e.target.value = formattedValue;
        });
      }

      const expiryInput = document.getElementById("expiryDate");
      if (expiryInput) {
        expiryInput.addEventListener("input", (e) => {
          let value = e.target.value.replace(/\D/g, "");
          if (value.length >= 2) {
            value = value.substr(0, 2) + "/" + value.substr(2, 2);
          }
          e.target.value = value;
        });
      }
    }, 100);

    return step;
  }

  createConfirmationStep() {
    const step = document.createElement("div");
    step.className = "booking-step";

    step.innerHTML = `
      <div class="step-header">
        <h3>✅ Booking Confirmation</h3>
        <p>Review and confirm your booking</p>
      </div>
      
      <div class="confirmation-content">
        <div class="success-animation">
          <div class="checkmark-circle">
            <div class="checkmark"></div>
          </div>
        </div>
        
        <div class="booking-summary">
          <h4>Booking Details</h4>
          <div class="summary-card">
            <div class="booking-id">
              <strong>Booking ID: ${this.generateBookingId()}</strong>
            </div>
            <div class="service-summary-final">
              <div class="service-icon">${this.getServiceIcon()}</div>
              <div class="service-details-final">
                <h5>${this.bookingData.title}</h5>
                ${
                  this.bookingData.from && this.bookingData.to
                    ? `
                  <p><strong>Route:</strong> ${this.bookingData.from} → ${this.bookingData.to}</p>
                `
                    : ""
                }
                ${
                  this.bookingData.date
                    ? `
                  <p><strong>Date:</strong> ${new Date(
                    this.bookingData.date
                  ).toLocaleDateString()}</p>
                `
                    : ""
                }
                <p><strong>Total Paid:</strong> <span class="final-price">₹${this.calculateTotalPrice()}</span></p>
              </div>
            </div>
          </div>
        </div>
        
        <div class="next-steps">
          <h4>What's Next?</h4>
          <div class="steps-list">
            <div class="next-step">
              <div class="step-icon">📧</div>
              <div class="step-text">
                <strong>Confirmation Email</strong>
                <p>You'll receive a confirmation email within 5 minutes</p>
              </div>
            </div>
            <div class="next-step">
              <div class="step-icon">📱</div>
              <div class="step-text">
                <strong>Booking Management</strong>
                <p>View and manage your booking in upcoming bookings</p>
              </div>
            </div>
            <div class="next-step">
              <div class="step-icon">🔔</div>
              <div class="step-text">
                <strong>Travel Reminders</strong>
                <p>We'll send you timely travel reminders</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    `;

    return step;
  }

  createFooter() {
    const footer = document.createElement("div");
    footer.className = "booking-footer";

    const backBtn = document.createElement("button");
    backBtn.className = "booking-btn booking-btn-secondary";
    backBtn.textContent = this.currentStep === 1 ? "Cancel" : "Back";
    backBtn.onclick = () => {
      if (this.currentStep === 1) {
        this.closeModal();
      } else {
        this.previousStep();
      }
    };

    const nextBtn = document.createElement("button");
    nextBtn.className = "booking-btn booking-btn-primary";
    nextBtn.id = "nextStepBtn";

    if (this.currentStep === 4) {
      nextBtn.innerHTML = `
        <div class="btn-content">
          <span>View Bookings</span>
          <span class="btn-icon">📅</span>
        </div>
      `;
      nextBtn.onclick = () => this.completeBooking();
    } else if (this.currentStep === 3) {
      nextBtn.innerHTML = `
        <div class="btn-content">
          <span>Pay ₹${this.calculateTotalPrice()}</span>
          <span class="btn-icon">💳</span>
        </div>
      `;
      nextBtn.onclick = () => this.processPayment();
    } else {
      nextBtn.innerHTML = `
        <div class="btn-content">
          <span>Continue</span>
          <span class="btn-icon">→</span>
        </div>
      `;
      nextBtn.onclick = () => this.nextStep();
    }

    footer.appendChild(backBtn);
    footer.appendChild(nextBtn);

    return footer;
  }

  nextStep() {
    if (this.validateCurrentStep()) {
      this.currentStep++;
      this.updateModal();
    }
  }

  previousStep() {
    this.currentStep--;
    this.updateModal();
  }

  processPayment() {
    if (this.validateCurrentStep()) {
      const nextBtn = document.getElementById("nextStepBtn");
      nextBtn.innerHTML = '<div class="loading-spinner"></div> Processing...';
      nextBtn.disabled = true;
      setTimeout(() => {
        this.nextStep();
      }, 2000);
    }
  }

  async completeBooking() {
    const booking = this.createBookingRecord();
    const bookings = JSON.parse(
      localStorage.getItem("travelmate_bookings") || "[]"
    );
    bookings.push(booking);
    localStorage.setItem("travelmate_bookings", JSON.stringify(bookings));

    try {
      const backendResult = await this.saveBookingToBackend(booking);
      if (backendResult?.booking) {
        booking.bookingRef = backendResult.booking.booking_ref || booking.bookingRef;
        booking.username = backendResult.booking.username || booking.username;
        booking.fullname = backendResult.booking.fullname || booking.fullname;
        booking.amount = backendResult.booking.amount || booking.amount;
        bookings[bookings.length - 1] = booking;
        localStorage.setItem("travelmate_bookings", JSON.stringify(bookings));
      }
    } catch (error) {
      console.error("Backend booking save failed:", error);
    }

    this.closeModal();
    showAlertModal(
      `${booking.title} booked successfully!\n\nBooking ID: ${
        booking.bookingRef || booking.id
      }`,
      "success",
      () => {
        window.location.href = "upcoming.html?new=true";
      }
    );
  }

  async saveBookingToBackend(booking) {
    const currentUser = JSON.parse(
      localStorage.getItem("travelmate_current_user") || "null"
    );
    const userId = currentUser?.id || currentUser?.user_id;

    if (!userId) {
      throw new Error("Current user is missing a database id");
    }

    const normalizedAmount = Number(
      String(booking.price || "0").replace(/[^\d.]/g, "")
    );
    const travelDate = booking.travelDate
      ? new Date(booking.travelDate).toISOString().split("T")[0]
      : new Date().toISOString().split("T")[0];

    const BASE_URL = window.location.origin;
    const response = await fetch(`${BASE_URL}/api/booking`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        user_id: userId,
        booking_type: booking.type,
        from_location: booking.from || "",
        to_location: booking.to || "",
        travel_date: travelDate,
        passengers: booking.passengers?.length || 1,
        amount: normalizedAmount || 0,
        status: booking.status || "confirmed",
      }),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.error || "Booking API request failed");
    }

    return response.json();
  }

  validateCurrentStep() {
    switch (this.currentStep) {
      case 1:
        return true;
      case 2:
        return this.validatePassengerDetails();
      case 3:
        return this.validatePaymentDetails();
      default:
        return true;
    }
  }

  validatePassengerDetails() {
    const totalTravelers = parseInt(this.bookingData.travelers) || 1;
    const missingFields = [];

    for (let i = 1; i <= totalTravelers; i++) {
      // All passengers need first name, last name, age, gender
      const baseFields = [`firstName_${i}`, `lastName_${i}`, `age_${i}`, `gender_${i}`];
      // Only primary passenger needs email & phone
      if (i === 1) {
        baseFields.push(`email_${i}`, `phone_${i}`);
      }

      baseFields.forEach((field) => {
        const element = document.getElementById(field);
        if (!element || !element.value.trim()) {
          missingFields.push(field);
          if (element) element.classList.add("error");
        } else {
          if (element) element.classList.remove("error");
        }
      });

      if (this.bookingData.type === "flight") {
        const docFields = [`documentType_${i}`, `documentNumber_${i}`];
        docFields.forEach((field) => {
          const element = document.getElementById(field);
          if (!element || !element.value.trim()) {
            missingFields.push(field);
            if (element) element.classList.add("error");
          } else {
            if (element) element.classList.remove("error");
          }
        });
      }
    }

    if (missingFields.length > 0) {
      this.showValidationError("Please fill in all required fields for every passenger");
      return false;
    }

    const emailEl = document.getElementById("email_1");
    if (emailEl) {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailRegex.test(emailEl.value)) {
        emailEl.classList.add("error");
        this.showValidationError("Please enter a valid email address");
        return false;
      }
    }

    return true;
  }

  validatePaymentDetails() {
    const paymentMethod = document.querySelector(
      'input[name="paymentMethod"]:checked'
    ).value;

    if (paymentMethod === "card") {
      const requiredFields = ["cardNumber", "expiryDate", "cvv", "cardName"];
      const missingFields = [];

      requiredFields.forEach((field) => {
        const element = document.getElementById(field);
        if (!element || !element.value.trim()) {
          missingFields.push(field);
          if (element) element.classList.add("error");
        } else {
          if (element) element.classList.remove("error");
        }
      });

      if (missingFields.length > 0) {
        this.showValidationError("Please fill in all card details");
        return false;
      }

      const cardNumber = document
        .getElementById("cardNumber")
        .value.replace(/\s/g, "");
      if (cardNumber.length < 13 || cardNumber.length > 19) {
        document.getElementById("cardNumber").classList.add("error");
        this.showValidationError("Please enter a valid card number");
        return false;
      }
    }

    return true;
  }

  showValidationError(message) {
    let errorDiv = document.querySelector(".validation-error");
    if (!errorDiv) {
      errorDiv = document.createElement("div");
      errorDiv.className = "validation-error";
      document.getElementById("booking-content").prepend(errorDiv);
    }

    errorDiv.textContent = message;
    errorDiv.style.display = "block";

    setTimeout(() => {
      if (errorDiv) errorDiv.style.display = "none";
    }, 5000);
  }

  updateModal() {
    const modal = document.querySelector(".booking-modal");
    if (modal) {
      const progressBar = modal.querySelector(".progress-container");
      progressBar.replaceWith(this.createProgressBar());

      const content = modal.querySelector(".booking-content");
      content.replaceWith(this.createStepContent());

      const footer = modal.querySelector(".booking-footer");
      footer.replaceWith(this.createFooter());
    }
  }

  calculateTotalPrice() {
    let basePrice = parseInt(
      this.bookingData.price?.replace(/[₹,]/g, "") || "0"
    );
    let total = basePrice;

    total += Math.round(basePrice * 0.1);

    const insurance = document.getElementById("insurance");
    const priority = document.getElementById("priority");
    const meal = document.getElementById("meal");
    const baggage = document.getElementById("baggage");

    if (insurance && insurance.checked) total += 199;
    if (priority && priority.checked) total += 99;
    if (meal && meal.checked) total += 299;
    if (baggage && baggage.checked) total += 499;

    return total;
  }

  generateBookingId() {
    const prefix = this.bookingData.type?.toUpperCase().substr(0, 2) || "BK";
    return `${prefix}${Date.now()}`;
  }

  getServiceIcon() {
    const icons = {
      flight: "✈️",
      train: "🚂",
      bus: "🚌",
      cab: "🚗",
      cruise: "🚢",
      hotel: "🏨",
    };
    return icons[this.bookingData.type] || "🎫";
  }

  createBookingRecord() {
    const user = JSON.parse(
      localStorage.getItem("travelmate_current_user") || "null"
    );
    const totalTravelers = parseInt(this.bookingData.travelers) || 1;

    const passengers = [];
    for (let i = 1; i <= totalTravelers; i++) {
      passengers.push({
        firstName: document.getElementById(`firstName_${i}`)?.value || "",
        lastName: document.getElementById(`lastName_${i}`)?.value || "",
        email: i === 1 ? (document.getElementById(`email_${i}`)?.value || "") : "",
        phone: i === 1 ? (document.getElementById(`phone_${i}`)?.value || "") : "",
        age: document.getElementById(`age_${i}`)?.value || "",
        gender: document.getElementById(`gender_${i}`)?.value || "",
      });
    }

    return {
      id: this.generateBookingId(),
      userId: user?.id || "guest",
      type: this.bookingData.type || "service",
      title: this.bookingData.title || "Service Booking",
      from: this.bookingData.from || "",
      to: this.bookingData.to || "",
      date: new Date().toISOString(),
      travelDate: this.bookingData.date || "",
      price: `₹${this.calculateTotalPrice()}`,
      status: "confirmed",
      passengers: passengers,
      passenger: passengers[0], // keep for backward compatibility
      paymentMethod:
        document.querySelector('input[name="paymentMethod"]:checked')?.value ||
        "card",
      bookingTime: new Date().toISOString(),
    };
  }

  closeModal() {
    const overlay = document.getElementById("booking-overlay");
    if (overlay) {
      overlay.classList.remove("show");
      setTimeout(() => {
        if (overlay.parentNode) {
          overlay.parentNode.removeChild(overlay);
        }
      }, 300);
    }
  }
}
window.bookingModal = new BookingModal();
window.showBookingModal = (serviceData) => {
  return window.bookingModal.showBookingModal(serviceData);
};
