// ===============================
// 1. Get elements from the page
// ===============================
const resultEl = document.getElementById("result");
const historyEl = document.getElementById("history");
const buttons = document.querySelectorAll(".keys button");

const themeBtn = document.getElementById("themeBtn");
const historyPanel = document.getElementById("historyPanel");
const historyList = document.getElementById("historyList");
const clearHistoryBtn = document.getElementById("clearHistoryBtn");

// ===============================
// 2. Calculator memory (state)
// ===============================
let expression = "";        // what the user typed, e.g. "2+2"
let historyText = "";       // small line, e.g. "2 + 2 ="
let justEvaluated = false;  // true right after pressing "="
let hasError = false;       // true when the math was invalid

const OPERATORS = ["+", "−", "×", "÷"];

// History memory: a list of { expression, answer }
const HISTORY_KEY = "calc-history";
const THEME_KEY = "calc-theme";
const MAX_HISTORY = 50;
let calcHistory = [];

// ===============================
// 3. Small helper functions
// ===============================
function lastChar() {
  return expression.slice(-1);
}

function isDigit(char) {
  return /^[0-9]$/.test(char);
}

// Does the expression end with something that holds a value?
function endsWithValue() {
  const char = lastChar();
  return isDigit(char) || char === "." || char === ")" || char === "%";
}

// After "=", typing a number starts a new calculation
function startFresh() {
  if (justEvaluated) {
    expression = "";
    justEvaluated = false;
  }
}

// Add spaces around operators: 2+2 -> 2 + 2
function formatExpression(text) {
  return text.replace(/([\d.)%])([+−×÷])/g, "$1 $2 ");
}

// ===============================
// 4. Show things on the screen
// ===============================
function updateDisplay() {
  const text = hasError ? "Error" : formatExpression(expression) || "0";

  resultEl.textContent = text;
  historyEl.textContent = historyText;

  resultEl.classList.toggle("text-danger", hasError);
  resultEl.classList.toggle("small-text", text.length > 12);
  resultEl.scrollLeft = resultEl.scrollWidth;
}

// ===============================
// 5. Adding things to the expression
// ===============================
function addNumber(num) {
  startFresh();
  if (lastChar() === ")" || lastChar() === "%") expression += "×";

  const currentNumber = expression.match(/[\d.]*$/)[0];
  if (currentNumber === "0") expression = expression.slice(0, -1);

  expression += num;
}

function addDecimal() {
  startFresh();
  if (lastChar() === ")" || lastChar() === "%") expression += "×";

  const currentNumber = expression.match(/[\d.]*$/)[0];
  if (currentNumber.includes(".")) return;
  expression += currentNumber === "" ? "0." : ".";
}

function addOperator(op) {
  justEvaluated = false;
  const last = lastChar();
  const before = expression.slice(-2, -1);

  if (expression === "" || last === "(") {
    if (op === "−") expression += op;
    return;
  }

  if (OPERATORS.includes(last)) {
    if (before === "" || before === "(") return;
    expression = expression.slice(0, -1) + op;
    return;
  }

  expression += op;
}

function addOpenBracket() {
  startFresh();
  if (endsWithValue()) expression += "×";
  expression += "(";
}

function addCloseBracket() {
  justEvaluated = false;
  const opens = (expression.match(/\(/g) || []).length;
  const closes = (expression.match(/\)/g) || []).length;

  if (opens > closes && endsWithValue()) expression += ")";
}

function addPercent() {
  justEvaluated = false;
  if (endsWithValue()) expression += "%";
}

function handleInput(value) {
  if (hasError) {
    hasError = false;
    expression = "";
  }
  historyText = "";

  if (isDigit(value)) addNumber(value);
  else if (value === ".") addDecimal();
  else if (OPERATORS.includes(value)) addOperator(value);
  else if (value === "(") addOpenBracket();
  else if (value === ")") addCloseBracket();
  else if (value === "%") addPercent();

  updateDisplay();
}

// ===============================
// 6. AC and DEL
// ===============================
function clearAll() {
  expression = "";
  historyText = "";
  justEvaluated = false;
  hasError = false;
  updateDisplay();
}

function deleteLast() {
  if (hasError || justEvaluated) {
    clearAll();
    return;
  }
  expression = expression.slice(0, -1);
  updateDisplay();
}

// ===============================
// 7. The math engine (no eval!)
// ===============================
function evaluate(text) {
  const tokens = text
    .replace(/×/g, "*")
    .replace(/÷/g, "/")
    .replace(/−/g, "-")
    .match(/\d+\.?\d*|\.\d+|[+\-*/()%]/g);

  if (!tokens) throw new Error("Empty");

  let position = 0;

  // + and − (lowest priority)
  function parseExpression() {
    let value = parseTerm();
    while (tokens[position] === "+" || tokens[position] === "-") {
      const op = tokens[position++];
      const right = parseTerm();
      value = op === "+" ? value + right : value - right;
    }
    return value;
  }

  // × and ÷ (higher priority)
  function parseTerm() {
    let value = parseFactor();
    while (tokens[position] === "*" || tokens[position] === "/") {
      const op = tokens[position++];
      const right = parseFactor();
      if (op === "/" && right === 0) throw new Error("Divide by zero");
      value = op === "*" ? value * right : value / right;
    }
    return value;
  }

  // Negative or positive sign, like -5
  function parseFactor() {
    if (tokens[position] === "-") {
      position++;
      return -parseFactor();
    }
    if (tokens[position] === "+") {
      position++;
      return parseFactor();
    }
    return parsePrimary();
  }

  // A number or a bracket, optionally followed by %
  function parsePrimary() {
    let value;

    if (tokens[position] === "(") {
      position++;
      value = parseExpression();
      if (tokens[position] !== ")") throw new Error("Missing )");
      position++;
    } else if (tokens[position] !== undefined && /^[\d.]/.test(tokens[position])) {
      value = parseFloat(tokens[position++]);
    } else {
      throw new Error("Unexpected token");
    }

    while (tokens[position] === "%") {
      position++;
      value = value / 100;
    }
    return value;
  }

  const answer = parseExpression();
  if (position < tokens.length) throw new Error("Unexpected token");
  return answer;
}

function formatNumber(number) {
  if (!isFinite(number) || Math.abs(number) > 1e15) throw new Error("Too large");

  let text = number.toFixed(10).replace(/\.?0+$/, "");
  if (text === "" || text === "-0") text = "0";
  return text.replace("-", "−");
}

// ===============================
// 8. The "=" button
// ===============================
function calculate() {
  if (hasError || expression === "" || justEvaluated) return;

  let expr = expression;

  // Remove operators at the end: "5+" -> "5"
  while (OPERATORS.includes(expr.slice(-1))) expr = expr.slice(0, -1);

  // Close any open brackets automatically
  const opens = (expr.match(/\(/g) || []).length;
  const closes = (expr.match(/\)/g) || []).length;
  expr += ")".repeat(Math.max(0, opens - closes));

  try {
    const answer = formatNumber(evaluate(expr));
    historyText = formatExpression(expr) + " =";
    expression = answer;
    justEvaluated = true;

    addToHistory(formatExpression(expr), answer); // NEW: save in history
  } catch (error) {
    hasError = true;
    expression = "";
    historyText = "";
  }

  updateDisplay();
}

// ===============================
// 9. Calculation history (NEW)
// ===============================

// Load the saved history when the page opens
function loadHistory() {
  try {
    const saved = JSON.parse(localStorage.getItem(HISTORY_KEY));
    if (Array.isArray(saved)) calcHistory = saved;
  } catch (error) {
    calcHistory = []; // storage blocked or broken data: start empty
  }
}

// Save the history in the browser so it stays after refresh
function saveHistory() {
  try {
    localStorage.setItem(HISTORY_KEY, JSON.stringify(calcHistory));
  } catch (error) {
    // storage not available: history still works until the page closes
  }
}

function addToHistory(exprText, answer) {
  calcHistory.unshift({ expression: exprText, answer: answer }); // newest first
  calcHistory = calcHistory.slice(0, MAX_HISTORY);
  saveHistory();
  renderHistory();
}

// Draw the list inside the side panel
function renderHistory() {
  historyList.innerHTML = "";

  if (calcHistory.length === 0) {
    const empty = document.createElement("li");
    empty.className = "list-group-item text-center text-secondary border-0";
    empty.textContent = "No calculations yet.";
    historyList.appendChild(empty);
    return;
  }

  calcHistory.forEach(function (item) {
    const li = document.createElement("li");
    li.className = "list-group-item history-item";

    const exprLine = document.createElement("div");
    exprLine.className = "history-expression";
    exprLine.textContent = item.expression + " =";

    const answerLine = document.createElement("div");
    answerLine.className = "history-answer";
    answerLine.textContent = item.answer;

    li.appendChild(exprLine);
    li.appendChild(answerLine);

    // Click an item: bring its answer back to the screen
    li.addEventListener("click", function () {
      hasError = false;
      expression = item.answer;
      historyText = item.expression + " =";
      justEvaluated = true;
      updateDisplay();

      const panel = bootstrap.Offcanvas.getInstance(historyPanel);
      if (panel) panel.hide();
    });

    historyList.appendChild(li);
  });
}

clearHistoryBtn.addEventListener("click", function () {
  calcHistory = [];
  saveHistory();
  renderHistory();
});

// ===============================
// 10. Dark / Light mode (NEW)
// ===============================
function applyTheme(theme) {
  // Bootstrap changes all its colors when this attribute changes
  document.documentElement.setAttribute("data-bs-theme", theme);
  themeBtn.textContent = theme === "dark" ? "☀️" : "🌙"; // shows what you switch TO

  try {
    localStorage.setItem(THEME_KEY, theme);
  } catch (error) {
    // ignore
  }
}

function loadTheme() {
  let theme = null;
  try {
    theme = localStorage.getItem(THEME_KEY);
  } catch (error) {
    // ignore
  }

  // No saved choice: follow the device setting
  if (theme !== "dark" && theme !== "light") {
    theme = window.matchMedia("(prefers-color-scheme: light)").matches ? "light" : "dark";
  }
  applyTheme(theme);
}

themeBtn.addEventListener("click", function () {
  const current = document.documentElement.getAttribute("data-bs-theme");
  applyTheme(current === "dark" ? "light" : "dark");
});

// ===============================
// 11. Listen for button clicks
// ===============================
buttons.forEach(function (button) {
  button.addEventListener("click", function () {
    const value = button.dataset.value;
    const action = button.dataset.action;

    if (value !== undefined) handleInput(value);
    else if (action === "equals") calculate();
    else if (action === "clear") clearAll();
    else if (action === "delete") deleteLast();
  });
});

// ===============================
// 12. Keyboard support
// ===============================
const keyMap = { "*": "×", "/": "÷", "-": "−", "+": "+" };

document.addEventListener("keydown", function (event) {
  if (event.ctrlKey || event.metaKey || event.altKey) return;

  // While the history panel is open, keys belong to the panel (Esc closes it)
  if (historyPanel.classList.contains("show")) return;

  const key = event.key;

  if (isDigit(key) || key === "." || key === "(" || key === ")" || key === "%") {
    handleInput(key);
  } else if (keyMap[key]) {
    handleInput(keyMap[key]);
  } else if (key === "Enter" || key === "=") {
    calculate();
  } else if (key === "Backspace") {
    deleteLast();
  } else if (key === "Escape") {
    clearAll();
  } else {
    return;
  }

  event.preventDefault();
});

// ===============================
// 13. Start the app
// ===============================
loadTheme();
loadHistory();
renderHistory();
updateDisplay();