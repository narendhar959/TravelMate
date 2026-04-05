(function() {
    const BASE_URL = window.location.origin;
    const API_URL = `${BASE_URL}/api/assistant-chat`;
    const STORAGE_PREFIX = "travelmate_ai_chat";
    const UPI_ID = "travelmate@upi";

    function defaultBookingState() {
        return {
            intent: "chat",
            booking_type: null,
            from_location: null,
            to_location: null,
            travel_date: null,
            passengers: null,
            stage: "collecting_details",
            quote_amount: null,
        };
    }

    class TravelMateAI {
        constructor(root) {
            this.root = root;
            this.toggle = root.querySelector("[data-ai-toggle]");
            this.form = root.querySelector("[data-ai-form]");
            this.input = root.querySelector("[data-ai-input]");
            this.messages = root.querySelector("[data-ai-messages]");
            this.sendButton = root.querySelector("[data-ai-send]");
            this.history = [];
            this.bookingState = defaultBookingState();
            this.isOpen = false;
            this.isLoading = false;
            this.typingNode = null;
            this.bindEvents();
            this.restoreSession();
            if (!this.history.length) {
                this.addMessage("assistant", this.getWelcomeMessage(), null, null, false);
            }
        }

        bindEvents() {
            this.toggle.addEventListener("click", () => this.togglePanel());
            this.form.addEventListener("submit", (event) => {
                event.preventDefault();
                this.handleSubmit();
            });
            this.input.addEventListener("keydown", (event) => {
                if (event.key === "Enter" && !event.shiftKey) {
                    event.preventDefault();
                    this.handleSubmit();
                }
            });
        }

        getCurrentUser() {
            try {
                return JSON.parse(localStorage.getItem("user") || "null");
            } catch (error) {
                return null;
            }
        }

        getStorageKey() {
            const user = this.getCurrentUser();
            const userKey =
                user?.id || user?.user_id || user?.username || user?.email || "guest";
            return `${STORAGE_PREFIX}_${userKey}`;
        }

        getUserName() {
            const currentUser = this.getCurrentUser();
            return (
                currentUser?.firstname ||
                currentUser?.username ||
                currentUser?.fullname ||
                "Traveler"
            );
        }

        getWelcomeMessage() {
            return (
                `Hi ${this.getUserName()}! I'm TravelMate AI. ` +
                "I can continue the conversation with you and help book flights, trains, buses, cabs, hotels, tours, cruises, and insurance."
            );
        }

        persistSession() {
            localStorage.setItem(
                this.getStorageKey(),
                JSON.stringify({
                    history: this.history,
                    bookingState: this.bookingState,
                    isOpen: this.isOpen,
                })
            );
        }

        restoreSession() {
            try {
                const raw = localStorage.getItem(this.getStorageKey());
                if (!raw) {
                    return;
                }
                const saved = JSON.parse(raw);
                this.history = Array.isArray(saved.history) ? saved.history : [];
                this.bookingState = saved.bookingState || defaultBookingState();
                this.isOpen = Boolean(saved.isOpen);

                this.messages.innerHTML = "";
                this.history.forEach((entry) => {
                    this.renderMessage(
                        entry.role,
                        entry.content,
                        entry.booking || null,
                        entry.card || null
                    );
                });

                this.root.classList.toggle("open", this.isOpen);
                this.root.classList.toggle("visible", this.isOpen);
                this.scrollToBottom();
            } catch (error) {
                this.clearConversationState(false);
            }
        }

        clearConversationState(shouldPersist = true) {
            this.history = [];
            this.bookingState = defaultBookingState();
            this.messages.innerHTML = "";
            if (shouldPersist) {
                localStorage.removeItem(this.getStorageKey());
            }
        }

        saveBookingToLocalStore(booking) {
            if (!booking) {
                return;
            }

            const currentUser = this.getCurrentUser();
            const existing = JSON.parse(localStorage.getItem("travelmate_bookings") || "[]");
            const localBooking = {
                id: booking.booking_ref || booking.id,
                bookingRef: booking.booking_ref || booking.id,
                userId: booking.user_id || currentUser?.id || currentUser?.user_id || "guest",
                type: booking.type || "service",
                title: `${(booking.type || "service").charAt(0).toUpperCase()}${(booking.type || "service").slice(1)} Booking`,
                from: booking.from || "",
                to: booking.to || "",
                date: booking.created_at || new Date().toISOString(),
                travelDate: booking.travel_date || "",
                time: booking.time || "",
                price: `Rs. ${booking.amount || 0}`,
                amount: booking.amount || 0,
                status: booking.status || "confirmed",
                fullname:
                    booking.fullname ||
                    currentUser?.fullname ||
                    [currentUser?.firstname, currentUser?.lastname].filter(Boolean).join(" "),
                username: booking.username || currentUser?.username || "",
            };

            const alreadyExists = existing.some((item) => {
                const ref = item.bookingRef || item.booking_ref || item.id;
                return String(ref) === String(localBooking.bookingRef);
            });

            if (!alreadyExists) {
                existing.unshift(localBooking);
                localStorage.setItem("travelmate_bookings", JSON.stringify(existing));
            }
        }

        togglePanel() {
            this.isOpen = !this.isOpen;
            this.root.classList.toggle("open", this.isOpen);
            window.requestAnimationFrame(() => {
                this.root.classList.toggle("visible", this.isOpen);
            });
            if (this.isOpen) {
                this.input.focus();
                this.scrollToBottom();
            } else {
                this.root.classList.remove("visible");
            }
            this.persistSession();
        }

        renderRichCard(container, card) {
            if (!card) {
                return;
            }

            const cardNode = document.createElement("div");
            cardNode.className = "ai-rich-card";

            if (card.type === "quote") {
                cardNode.innerHTML = `
          <div class="ai-rich-title">Trip Estimate</div>
          <div class="ai-price-line"><span>Plan</span><strong>${card.summary || "Travel booking"}</strong></div>
          ${card.time ? `<div class="ai-price-line"><span>Time</span><strong>${card.time}</strong></div>` : ""}
          <div class="ai-price-line"><span>Travelers</span><strong>${card.passengers || 1}</strong></div>
          <div class="ai-price-line ai-price-total"><span>Total Bill</span><strong>Rs. ${Number(card.amount || 0).toFixed(2)}</strong></div>
        `;
                if (card.showConfirm) {
                    const actions = document.createElement("div");
                    actions.className = "ai-action-row";
                    const okButton = document.createElement("button");
                    okButton.className = "ai-action-btn primary";
                    okButton.textContent = "OK, continue";
                    okButton.onclick = () => this.sendQuickReply("ok");
                    actions.appendChild(okButton);

                    const betterPriceButton = document.createElement("button");
                    betterPriceButton.className = "ai-action-btn secondary";
                    betterPriceButton.textContent = "Better price";
                    betterPriceButton.onclick = () => this.sendQuickReply("show better price");
                    actions.appendChild(betterPriceButton);

                    const modifyButton = document.createElement("button");
                    modifyButton.className = "ai-action-btn secondary";
                    modifyButton.textContent = "Modify plan";
                    modifyButton.onclick = () => this.sendQuickReply("change booking");
                    actions.appendChild(modifyButton);

                    cardNode.appendChild(actions);
                }
            }

            if (card.type === "payment") {
                cardNode.innerHTML = `
          <div class="ai-rich-title">UPI Payment</div>
          <div class="ai-payment-grid">
            <div class="ai-upi-box">
              <div>Pay securely using any UPI app.</div>
              <div class="ai-upi-id">${UPI_ID}</div>
              <div class="ai-price-line ai-price-total"><span>Amount</span><strong>Rs. ${Number(card.amount || 0).toFixed(2)}</strong></div>
            </div>
            <div class="ai-qr-box" aria-label="UPI QR code"></div>
          </div>
        `;

                const actions = document.createElement("div");
                actions.className = "ai-action-row";
                const payButton = document.createElement("button");
                payButton.className = "ai-action-btn primary";
                payButton.textContent = "Pay now";
                payButton.onclick = () => this.startPayment(payButton);
                actions.appendChild(payButton);

                const cancelButton = document.createElement("button");
                cancelButton.className = "ai-action-btn secondary";
                cancelButton.textContent = "Change details";
                cancelButton.onclick = () => this.sendQuickReply("change booking");
                actions.appendChild(cancelButton);
                cardNode.appendChild(actions);
            }

            container.appendChild(cardNode);
        }

        renderMessage(role, text, booking, card) {
            const message = document.createElement("div");
            message.className = `ai-message ${role}`;
            message.textContent = text;

            if (booking) {
                const meta = document.createElement("div");
                meta.className = "ai-message-meta";
                meta.textContent = `Ref: ${booking.booking_ref} | Date: ${booking.travel_date}`;
                message.appendChild(meta);

                const chip = document.createElement("div");
                chip.className = "ai-booking-chip";
                chip.textContent = `${booking.type.toUpperCase()} booked for ${booking.passengers} traveler${booking.passengers > 1 ? "s" : ""}`;
                message.appendChild(chip);
            }

            this.renderRichCard(message, card);
            this.messages.appendChild(message);
            this.scrollToBottom();
        }

        addMessage(role, text, booking = null, card = null, persist = true) {
            this.renderMessage(role, text, booking, card);
            this.history.push({ role, content: text, booking, card });
            if (persist) {
                this.persistSession();
            }
        }

        showTyping() {
            this.removeTyping();
            this.typingNode = document.createElement("div");
            this.typingNode.className = "ai-message assistant";
            this.typingNode.innerHTML =
                '<div class="ai-typing"><span></span><span></span><span></span></div>';
            this.messages.appendChild(this.typingNode);
            this.scrollToBottom();
        }

        removeTyping() {
            if (this.typingNode && this.typingNode.parentNode) {
                this.typingNode.parentNode.removeChild(this.typingNode);
            }
            this.typingNode = null;
        }

        async sendQuickReply(text) {
            this.input.value = text;
            await this.handleSubmit();
        }

        async startPayment(button) {
            if (this.isLoading) {
                return;
            }

            const original = button.innerHTML;
            button.disabled = true;
            button.innerHTML = '<span class="ai-spinner"></span> Processing';

            await new Promise((resolve) => setTimeout(resolve, 1800));

            try {
                await this.sendAssistantRequest("__PAY_NOW__", true, false);
            } finally {
                button.disabled = false;
                button.innerHTML = original;
            }
        }

        async handleSubmit() {
            const message = this.input.value.trim();
            if (!message || this.isLoading) {
                return;
            }

            this.input.value = "";
            await this.sendAssistantRequest(message, false, true);
        }

        async sendAssistantRequest(message, paymentAction = false, echoUserMessage = true) {
            if (echoUserMessage) {
                this.addMessage("user", message);
            }

            this.isLoading = true;
            this.sendButton.disabled = true;
            this.showTyping();

            const currentUser = this.getCurrentUser();

            try {
                const response = await fetch(API_URL, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({
                        message,
                        payment_action: paymentAction,
                        user_id: currentUser?.id || currentUser?.user_id || null,
                        user_profile: currentUser,
                        history: this.history.slice(-12),
                        booking_state: this.bookingState,
                    }),
                });

                const data = await response.json().catch(() => ({}));
                this.removeTyping();

                if (!response.ok) {
                    this.addMessage(
                        "assistant",
                        data.reply || data.error || "I hit a problem while processing that request."
                    );
                    return;
                }

                this.bookingState = data.booking_state || defaultBookingState();
                if (data.booking) {
                    this.saveBookingToLocalStore(data.booking);
                }

                if (data.chat_closed) {
                    const finalReply = data.reply || "Thanks for chatting.";
                    this.clearConversationState(false);
                    this.addMessage("assistant", finalReply, null, null, false);
                    this.persistSession();
                    setTimeout(() => {
                        this.clearConversationState(true);
                        this.addMessage("assistant", this.getWelcomeMessage(), null, null, false);
                        this.persistSession();
                    }, 0);
                    return;
                }

                let card = null;
                if (data.quote_ready) {
                    card = {
                        type: "quote",
                        amount: data.quote_amount,
                        summary: data.booking_summary,
                        time: this.bookingState.travel_time,
                        passengers: this.bookingState.passengers,
                        showConfirm: true,
                    };
                } else if (data.payment_required) {
                    card = {
                        type: "payment",
                        amount: data.quote_amount || this.bookingState.quote_amount,
                    };
                }

                this.addMessage("assistant", data.reply || "I am here to help.", data.booking || null, card);
            } catch (error) {
                this.removeTyping();
                this.addMessage(
                    "assistant",
                    "I could not reach the AI service. Please make sure the separate Ollama assistant backend is running on port 5001."
                );
            } finally {
                this.isLoading = false;
                this.sendButton.disabled = false;
            }
        }

        scrollToBottom() {
            this.messages.scrollTop = this.messages.scrollHeight;
        }
    }

    document.addEventListener("DOMContentLoaded", function() {
        const root = document.querySelector("[data-ai-assistant]");
        if (!root) {
            return;
        }
        window.travelMateAI = new TravelMateAI(root);
    });
})();
