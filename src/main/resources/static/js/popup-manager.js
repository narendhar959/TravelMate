class PopupManager {
  constructor() {
    this.currentPopup = null;
    this.init();
  }

  init() {
    if (!document.getElementById("popup-styles")) {
      const link = document.createElement("link");
      link.id = "popup-styles";
      link.rel = "stylesheet";
      link.href = "/css/popup-styles.css";
      document.head.appendChild(link);
    }
  }

  createPopup(config) {
    this.closePopup();

    const overlay = document.createElement("div");
    overlay.className = "popup-overlay";
    overlay.id = "popup-overlay";

    const modal = document.createElement("div");
    modal.className = "popup-modal";

    const closeBtn = document.createElement("button");
    closeBtn.className = "popup-close";
    closeBtn.innerHTML = "x";
    closeBtn.onclick = () => this.closePopup();
    const header = document.createElement("div");
    header.className = "popup-header";

    if (config.icon) {
      const icon = document.createElement("div");
      icon.className = "popup-icon";
      icon.innerHTML = config.icon;
      header.appendChild(icon);
    }

    if (config.title) {
      const title = document.createElement("h2");
      title.className = "popup-title";
      title.textContent = config.title;
      header.appendChild(title);
    }

    if (config.subtitle) {
      const subtitle = document.createElement("p");
      subtitle.className = "popup-subtitle";
      subtitle.textContent = config.subtitle;
      header.appendChild(subtitle);
    }

    const content = document.createElement("div");
    content.className = "popup-content";

    if (config.message) {
      const message = document.createElement("p");
      message.innerHTML = config.message;
      content.appendChild(message);
    }

    if (config.details) {
      const details = document.createElement("div");
      details.className = "popup-details";

      config.details.forEach((detail) => {
        const row = document.createElement("div");
        row.className = "detail-row";

        const label = document.createElement("span");
        label.className = "detail-label";
        label.textContent = detail.label;

        const value = document.createElement("span");
        value.className = "detail-value";
        value.innerHTML = detail.value;

        row.appendChild(label);
        row.appendChild(value);
        details.appendChild(row);
      });

      content.appendChild(details);
    }

    const actions = document.createElement("div");
    actions.className = "popup-actions";

    if (config.buttons) {
      config.buttons.forEach((button) => {
        const btn = document.createElement("button");
        btn.className = `popup-btn ${button.class || "popup-btn-secondary"}`;
        btn.textContent = button.text;
        btn.onclick = () => {
          if (button.action) button.action();
          if (button.close !== false) this.closePopup();
        };
        actions.appendChild(btn);
      });
    } else {
      const okBtn = document.createElement("button");
      okBtn.className = "popup-btn popup-btn-primary";
      okBtn.textContent = "OK";
      okBtn.onclick = () => this.closePopup();
      actions.appendChild(okBtn);
    }

    modal.appendChild(closeBtn);
    modal.appendChild(header);
    modal.appendChild(content);
    modal.appendChild(actions);
    overlay.appendChild(modal);

    document.body.appendChild(overlay);
    this.currentPopup = overlay;
    setTimeout(() => {
      overlay.classList.add("show");
    }, 10);

    setTimeout(() => {
      this.closePopup();
    }, 900000);

    overlay.addEventListener("click", (e) => {
      if (e.target === overlay) {
        this.closePopup();
      }
    });

    const escapeHandler = (e) => {
      if (e.key === "Escape") {
        this.closePopup();
        document.removeEventListener("keydown", escapeHandler);
      }
    };
    document.addEventListener("keydown", escapeHandler);

    return overlay;
  }

  closePopup() {
    if (this.currentPopup) {
      this.currentPopup.classList.remove("show");
      setTimeout(() => {
        if (this.currentPopup && this.currentPopup.parentNode) {
          this.currentPopup.parentNode.removeChild(this.currentPopup);
        }
        this.currentPopup = null;
      }, 300000);
    }
  }

  // Preset popup types
  showSuccess(title, message, details = null, callback = null) {
    return this.createPopup({
      icon: '<svg class="success-checkmark" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 52 52"><circle class="success-checkmark-circle" cx="26" cy="26" r="25" fill="none"/><path class="success-checkmark-check" fill="none" d="m14.1 27.2l7.1 7.2 16.7-16.8"/></svg>',
      title: title,
      message: message,
      details: details,
      buttons: [
        {
          text: "Great!",
          class: "popup-btn-success",
          action: callback,
        },
      ],
    });
  }

  showBookingSuccess(bookingData, callback = null) {
    const details = [
      { label: "Booking ID", value: `<strong>${bookingData.id}</strong>` },
      { label: "Service", value: bookingData.title },
      {
        label: "Price",
        value: `<strong style="color: #28a745;">${bookingData.price}</strong>`,
      },
    ];

    if (bookingData.travelDate) {
      details.push({
        label: "Travel Date",
        value: new Date(bookingData.travelDate).toLocaleDateString(),
      });
    }

    if (bookingData.from && bookingData.to) {
      details.push({
        label: "Route",
        value: `${bookingData.from} → ${bookingData.to}`,
      });
    }

    return this.createPopup({
      icon: "🎉",
      title: "Booking Confirmed!",
      subtitle: "Your booking has been successfully confirmed",
      details: details,
      buttons: [
        {
          text: "View Ticket",
          class: "popup-btn-primary",
          action: () => {
            window.location.href = `ticket.html?id=${bookingData.id}&type=${bookingData.type}`;
          },
        },
        {
          text: "My Bookings",
          class: "popup-btn-secondary",
          action: () => {
            window.location.href = "upcoming.html";
          },
        },
      ],
    });
  }

  showError(title, message, callback = null) {
    return this.createPopup({
      icon: "⚠️",
      title: title,
      message: message,
      buttons: [
        {
          text: "OK",
          class: "popup-btn-secondary",
          action: callback,
        },
      ],
    });
  }

  showConfirm(title, message, onConfirm, onCancel = null) {
    return this.createPopup({
      icon: "❓",
      title: title,
      message: message,
      buttons: [
        {
          text: "Confirm",
          class: "popup-btn-primary",
          action: onConfirm,
        },
        {
          text: "Cancel",
          class: "popup-btn-secondary",
          action: onCancel,
        },
      ],
    });
  }

  showLoading(
    title = "Processing...",
    message = "Please wait while we process your request."
  ) {
    return this.createPopup({
      icon: '<div class="loading-spinner"></div>',
      title: title,
      message: message,
      buttons: [],
    });
  }

  showProfileSuccess(action, userData) {
    const actionMessages = {
      register: "Account Created Successfully!",
      login: "Welcome Back!",
      update: "Profile Updated Successfully!",
    };

    const actionSubtitles = {
      register: "Your TravelMate account has been created",
      login: `Welcome back, ${userData.firstname}!`,
      update: "Your profile information has been updated",
    };

    return this.createPopup({
      icon: action === "login" ? "👋" : "✅",
      title: actionMessages[action],
      subtitle: actionSubtitles[action],
      details: [
        { label: "Name", value: `${userData.firstname} ${userData.lastname}` },
        { label: "Email", value: userData.email },
        { label: "Phone", value: userData.phone || "Not provided" },
      ],
      buttons: [
        {
          text: action === "register" ? "Start Exploring" : "Continue",
          class: "popup-btn-success",
          action: () => {
            if (action === "register") {
              window.location.href = "main.html";
            }
          },
        },
      ],
    });
  }
}
window.popupManager = new PopupManager();

window.showSuccessPopup = (title, message, details, callback) => {
  return window.popupManager.showSuccess(title, message, details, callback);
};

window.showBookingPopup = (bookingData, callback) => {
  return window.popupManager.showBookingSuccess(bookingData, callback);
};

window.showErrorPopup = (title, message, callback) => {
  return window.popupManager.showError(title, message, callback);
};

window.showConfirmPopup = (title, message, onConfirm, onCancel) => {
  return window.popupManager.showConfirm(title, message, onConfirm, onCancel);
};

window.showLoadingPopup = (title, message) => {
  return window.popupManager.showLoading(title, message);
};

window.showProfilePopup = (action, userData) => {
  return window.popupManager.showProfileSuccess(action, userData);
};

window.closePopup = () => {
  return window.popupManager.closePopup();
};
