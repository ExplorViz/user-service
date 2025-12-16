// API Base URL
const API_BASE = window.location.origin;

// State
let currentUserId = null;
let allTokens = [];
let filteredTokens = [];

// DOM Elements
let searchForm;
let userIdInput;
let loadAllBtn;
let tokensContainer;
let tokensGrid;
let emptyState;
let loadingIndicator;
let errorMessage;
let sectionTitle;

// Initialize Material Components
function initializeMaterialComponents() {
  const textFields = document.querySelectorAll(".mdc-text-field");
  textFields.forEach((textField) => {
    mdc.textField.MDCTextField.attachTo(textField);
  });

  const buttons = document.querySelectorAll(".mdc-button");
  buttons.forEach((button) => {
    mdc.ripple.MDCRipple.attachTo(button);
  });
}

// Initialize DOM Elements
function initializeDOMElements() {
  searchForm = document.getElementById("searchForm");
  userIdInput = document.getElementById("userIdInput");
  loadAllBtn = document.getElementById("loadAllBtn");
  tokensContainer = document.getElementById("tokensContainer");
  tokensGrid = document.getElementById("tokensGrid");
  emptyState = document.getElementById("emptyState");
  loadingIndicator = document.getElementById("loadingIndicator");
  errorMessage = document.getElementById("errorMessage");
  sectionTitle = document.getElementById("sectionTitle");
}

// Show error message
function showError(message) {
  errorMessage.textContent = message;
  errorMessage.classList.add("show");
  setTimeout(() => {
    errorMessage.classList.remove("show");
  }, 5000);
}

// Format date
function formatDate(timestamp) {
  if (!timestamp) return "N/A";
  const date = new Date(timestamp);
  return date.toLocaleString();
}

// Show toast notification
function showToast(message) {
  const toast = document.createElement("div");
  toast.className = "toast";
  toast.textContent = message;
  document.body.appendChild(toast);
  setTimeout(() => {
    toast.classList.add("slide-down");
    setTimeout(() => document.body.removeChild(toast), 300);
  }, 3000);
}

// Copy to clipboard
function copyToClipboard(text) {
  if (navigator.clipboard && navigator.clipboard.writeText) {
    navigator.clipboard
      .writeText(text)
      .then(() => {
        showToast("Copied to clipboard!");
      })
      .catch(() => {
        // Fallback for older browsers
        fallbackCopyToClipboard(text);
      });
  } else {
    fallbackCopyToClipboard(text);
  }
}

// Fallback copy method
function fallbackCopyToClipboard(text) {
  const textArea = document.createElement("textarea");
  textArea.value = text;
  textArea.style.position = "fixed";
  textArea.style.left = "-999999px";
  document.body.appendChild(textArea);
  textArea.select();
  try {
    document.execCommand("copy");
    showToast("Copied to clipboard!");
  } catch (err) {
    showToast("Failed to copy");
  }
  document.body.removeChild(textArea);
}

// Render tokens to the grid
function renderTokens(tokens) {
  tokensGrid.innerHTML = "";
  if (tokens.length === 0) {
    tokensContainer.classList.add("hidden");
    emptyState.classList.remove("hidden");
    emptyState.innerHTML = `
      <div class="empty-state-icon">
        <i class="material-icons">inbox</i>
      </div>
      <div class="empty-state-text">
        <strong>No tokens found</strong><br><br>
        ${
          currentUserId
            ? `No landscape tokens found for user: <strong>${currentUserId}</strong>`
            : "No landscape tokens available."
        }
      </div>
    `;
    return;
  }

  tokensContainer.classList.remove("hidden");
  emptyState.classList.add("hidden");
  sectionTitle.textContent = `Landscape Tokens (${tokens.length})`;

  tokens.forEach((token) => {
    tokensGrid.appendChild(createTokenCard(token));
  });
}

// Create token card
function createTokenCard(token) {
  const card = document.createElement("div");
  card.className = "token-card";

  const isOwned = currentUserId ? token.ownerId === currentUserId : false;
  const hasSecret = token.secret && token.secret.length > 0;

  // Escape HTML to prevent XSS
  const escapeHtml = (text) => {
    const div = document.createElement("div");
    div.textContent = text;
    return div.innerHTML;
  };

  const alias = escapeHtml(token.alias || "Unnamed Token");
  const tokenValue = escapeHtml(token.value);
  const tokenSecret = hasSecret ? escapeHtml(token.secret) : "";
  const ownerId = escapeHtml(token.ownerId);
  const sharedUsers =
    token.sharedUsersIds && token.sharedUsersIds.length > 0
      ? token.sharedUsersIds.map((id) => escapeHtml(id)).join(", ")
      : "";

  card.innerHTML = `
    <div class="token-card-header">
      <div>
        <div class="token-card-title">
          ${alias}
        </div>
        <div class="token-card-subtitle">Created: ${formatDate(token.created)}</div>
      </div>
    </div>
    <div class="token-info">
      <div class="token-info-label">Token Value</div>
      <div class="token-info-value">
        ${tokenValue}
        <i class="material-icons copy-button" onclick="copyToClipboard('${tokenValue.replace(
          /'/g,
          "\\'"
        )}')" title="Copy">content_copy</i>
      </div>
    </div>
    ${
      hasSecret
        ? `
    <div class="token-info">
      <div class="token-info-label">Secret</div>
      <div class="token-info-value">
        ${tokenSecret}
        <i class="material-icons copy-button" onclick="copyToClipboard('${tokenSecret.replace(
          /'/g,
          "\\'"
        )}')" title="Copy">content_copy</i>
      </div>
    </div>
    `
        : ""
    }
    <div class="token-info">
      <div class="token-info-label">Owner ID</div>
      <div class="token-info-value" ${isOwned && `style="font-weight: bold;"`}>${ownerId}</div>
    </div>
    ${
      sharedUsers
        ? `
    <div class="token-info">
      <div class="token-info-label">Shared With</div>
      <div class="token-info-value">${sharedUsers}</div>
    </div>
    `
        : ""
    }
  `;

  return card;
}

// Load all tokens
async function loadAllTokens() {
  currentUserId = null;
  loadingIndicator.classList.remove("hidden");
  tokensContainer.classList.add("hidden");
  emptyState.classList.add("hidden");
  userIdInput.value = "";

  try {
    const response = await fetch(`${API_BASE}/tokens`, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
      },
    });

    if (!response.ok) {
      throw new Error(`Failed to load tokens: ${response.status} ${response.statusText}`);
    }

    allTokens = await response.json();
    filteredTokens = allTokens;
    loadingIndicator.classList.add("hidden");
    renderTokens(filteredTokens);
  } catch (error) {
    loadingIndicator.classList.add("hidden");
    showError(`Error loading tokens: ${error.message}`);
    emptyState.classList.remove("hidden");
  }
}

// Load tokens for user
async function loadTokensForUser(userId) {
  currentUserId = userId;
  loadingIndicator.classList.remove("hidden");
  tokensContainer.classList.add("hidden");
  emptyState.classList.add("hidden");

  try {
    const response = await fetch(`${API_BASE}/user/${userId}/token`, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
      },
    });

    if (!response.ok) {
      throw new Error(`Failed to load tokens: ${response.status} ${response.statusText}`);
    }

    const tokens = await response.json();
    filteredTokens = tokens;
    loadingIndicator.classList.add("hidden");
    renderTokens(filteredTokens);
  } catch (error) {
    loadingIndicator.classList.add("hidden");
    showError(`Error loading tokens: ${error.message}`);
    emptyState.classList.remove("hidden");
  }
}

// Filter tokens by user ID from all tokens
function filterTokensByUserId(userId) {
  if (!userId || userId.trim() === "") {
    filteredTokens = allTokens;
    currentUserId = null;
  } else {
    currentUserId = userId.trim();
    filteredTokens = allTokens.filter(
      (token) =>
        token.ownerId === currentUserId ||
        (token.sharedUsersIds && token.sharedUsersIds.includes(currentUserId))
    );
  }
  renderTokens(filteredTokens);
}

// Event listeners setup
function setupEventListeners() {
  loadAllBtn.addEventListener("click", () => {
    loadAllTokens();
  });

  searchForm.addEventListener("submit", (e) => {
    e.preventDefault();
    const userId = userIdInput.value.trim();
    if (userId) {
      if (allTokens.length > 0) {
        // If we already have all tokens loaded, just filter them
        filterTokensByUserId(userId);
      } else {
        // Otherwise, fetch tokens for this user
        loadTokensForUser(userId);
      }
    } else {
      // If search is cleared and we have all tokens, show all again
      if (allTokens.length > 0) {
        filterTokensByUserId("");
      } else {
        showError('Please enter a User ID or click "Load All Tokens"');
      }
    }
  });
}

// Initialize application
function init() {
  initializeDOMElements();
  initializeMaterialComponents();
  setupEventListeners();
  loadAllTokens();
}

// Load all tokens on page load
window.addEventListener("DOMContentLoaded", init);
