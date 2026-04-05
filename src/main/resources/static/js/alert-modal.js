function showAlertModal(message, type = "info", onClose = null) {
  if (!document.getElementById("alert-modal-keyframes")) {
    const style = document.createElement("style");
    style.id = "alert-modal-keyframes";
    style.textContent = `
      @keyframes alertModalFadeIn {
        from {
          opacity: 0;
          transform: translateY(12px) scale(0.96);
        }
        to {
          opacity: 1;
          transform: translateY(0) scale(1);
        }
      }

      @keyframes alertModalFadeOut {
        from {
          opacity: 1;
          transform: translateY(0) scale(1);
        }
        to {
          opacity: 0;
          transform: translateY(12px) scale(0.96);
        }
      }
    `;
    document.head.appendChild(style);
  }

  const titleMap = {
    success: "Success",
    error: "Error",
    info: "Info",
  };

  const iconMap = {
    success: "🎉",
    error: "⚠️",
    info: "ℹ️",
  };

  const overlay = document.createElement("div");
  overlay.style.cssText = `
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: rgba(0, 0, 0, 0.58);
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 20px;
    box-sizing: border-box;
    z-index: 9999;
    opacity: 0;
    transition: opacity 0.2s ease;
  `;

  const modal = document.createElement("div");
  modal.style.cssText = `
    background: linear-gradient(180deg, #ffffff 0%, #f5fbfb 100%);
    padding: 25px;
    border-radius: 18px;
    width: min(350px, 100%);
    text-align: center;
    box-shadow: 0 18px 50px rgba(0, 0, 0, 0.24);
    border: 1px solid rgba(0, 128, 128, 0.16);
    animation: alertModalFadeIn 0.28s ease forwards;
    font-family: Arial, sans-serif;
  `;

  const title = document.createElement("h2");
  title.textContent = `${iconMap[type] || iconMap.info} ${
    titleMap[type] || titleMap.info
  }`;
  title.style.cssText = `
    margin: 0 0 12px;
    color: #008080;
    font-size: 1.5rem;
    font-weight: 700;
  `;

  const msg = document.createElement("p");
  msg.textContent = message;
  msg.style.cssText = `
    margin: 0;
    color: #355454;
    font-size: 1rem;
    line-height: 1.6;
    white-space: pre-line;
  `;

  const btn = document.createElement("button");
  btn.textContent = "OK";
  btn.style.cssText = `
    margin-top: 20px;
    padding: 10px 24px;
    border: none;
    background: #008080;
    color: white;
    border-radius: 10px;
    cursor: pointer;
    font-size: 0.95rem;
    font-weight: 600;
    box-shadow: 0 10px 22px rgba(0, 128, 128, 0.24);
    transition: transform 0.15s ease, box-shadow 0.15s ease;
  `;

  let isClosing = false;

  const closeModal = () => {
    if (isClosing) {
      return;
    }

    isClosing = true;
    modal.style.animation = "alertModalFadeOut 0.2s ease forwards";
    overlay.style.opacity = "0";
    setTimeout(() => {
      if (overlay.parentNode) {
        overlay.parentNode.removeChild(overlay);
      }
      if (typeof onClose === "function") {
        onClose();
      }
    }, 200);
  };

  btn.onmouseenter = () => {
    btn.style.transform = "translateY(-1px)";
    btn.style.boxShadow = "0 14px 28px rgba(0, 128, 128, 0.28)";
  };

  btn.onmouseleave = () => {
    btn.style.transform = "translateY(0)";
    btn.style.boxShadow = "0 10px 22px rgba(0, 128, 128, 0.24)";
  };

  btn.onclick = () => {
    document.removeEventListener("keydown", escapeHandler);
    closeModal();
  };

  overlay.addEventListener("click", (event) => {
    if (event.target === overlay) {
      document.removeEventListener("keydown", escapeHandler);
      closeModal();
    }
  });

  const escapeHandler = (event) => {
    if (event.key === "Escape") {
      document.removeEventListener("keydown", escapeHandler);
      closeModal();
    }
  };

  document.addEventListener("keydown", escapeHandler);

  modal.appendChild(title);
  modal.appendChild(msg);
  modal.appendChild(btn);
  overlay.appendChild(modal);
  document.body.appendChild(overlay);

  requestAnimationFrame(() => {
    overlay.style.opacity = "1";
  });
}
