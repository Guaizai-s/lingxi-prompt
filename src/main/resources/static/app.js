const state = {
  prompts: [],
  categories: [],
  tags: [],
  activeTag: "",
  editingId: null
};

const $ = (selector) => document.querySelector(selector);

const elements = {
  networkStatus: $("#networkStatus"),
  keywordInput: $("#keywordInput"),
  categoryFilter: $("#categoryFilter"),
  clearFiltersBtn: $("#clearFiltersBtn"),
  tagFilters: $("#tagFilters"),
  promptList: $("#promptList"),
  resultCount: $("#resultCount"),
  promptForm: $("#promptForm"),
  promptId: $("#promptId"),
  formTitle: $("#formTitle"),
  titleInput: $("#titleInput"),
  contentInput: $("#contentInput"),
  categoryInput: $("#categoryInput"),
  categoryOptions: $("#categoryOptions"),
  tagsInput: $("#tagsInput"),
  resetFormBtn: $("#resetFormBtn"),
  categoryForm: $("#categoryForm"),
  categoryNameInput: $("#categoryNameInput"),
  categoryList: $("#categoryList"),
  toast: $("#toast")
};

function showToast(message) {
  elements.toast.textContent = message;
  elements.toast.classList.add("show");
  window.clearTimeout(showToast.timer);
  showToast.timer = window.setTimeout(() => elements.toast.classList.remove("show"), 1800);
}

function setNetworkStatus() {
  const online = navigator.onLine;
  elements.networkStatus.textContent = online ? "在线" : "离线";
  elements.networkStatus.classList.toggle("offline", !online);
}

async function request(path, options = {}) {
  const response = await fetch(path, {
    headers: {
      "Content-Type": "application/json",
      ...(options.headers || {})
    },
    ...options
  });
  if (!response.ok) {
    const problem = await response.json().catch(() => ({}));
    throw new Error(problem.detail || "请求失败");
  }
  if (response.status === 204) {
    return null;
  }
  return response.json();
}

function buildQuery() {
  const params = new URLSearchParams();
  const keyword = elements.keywordInput.value.trim();
  const category = elements.categoryFilter.value;
  if (keyword) params.set("keyword", keyword);
  if (category) params.set("category", category);
  if (state.activeTag) params.set("tag", state.activeTag);
  return params.toString();
}

async function loadPrompts() {
  const query = buildQuery();
  try {
    state.prompts = await request(`/api/prompts${query ? `?${query}` : ""}`);
    localStorage.setItem("lingxi.prompts.cache", JSON.stringify(state.prompts));
  } catch (error) {
    const cached = localStorage.getItem("lingxi.prompts.cache");
    state.prompts = cached ? JSON.parse(cached) : [];
    showToast(navigator.onLine ? error.message : "离线模式：显示本地缓存");
  }
  renderPrompts();
}

async function loadOptions() {
  try {
    const [categories, tags] = await Promise.all([
      request("/api/categories"),
      request("/api/tags")
    ]);
    state.categories = categories;
    state.tags = tags;
    localStorage.setItem("lingxi.categories.cache", JSON.stringify(categories));
    localStorage.setItem("lingxi.tags.cache", JSON.stringify(tags));
  } catch (error) {
    state.categories = JSON.parse(localStorage.getItem("lingxi.categories.cache") || "[]");
    state.tags = JSON.parse(localStorage.getItem("lingxi.tags.cache") || "[]");
  }
  renderCategoryOptions();
  renderTags();
  renderCategoryManager();
}

function renderCategoryOptions() {
  const selected = elements.categoryFilter.value;
  elements.categoryFilter.innerHTML = `<option value="">全部分类</option>`;
  elements.categoryOptions.innerHTML = "";
  state.categories.forEach((category) => {
    const option = document.createElement("option");
    option.value = category;
    option.textContent = category;
    elements.categoryFilter.appendChild(option);

    const dataOption = document.createElement("option");
    dataOption.value = category;
    elements.categoryOptions.appendChild(dataOption);
  });
  elements.categoryFilter.value = selected;
}

function renderTags() {
  elements.tagFilters.innerHTML = "";
  if (state.tags.length === 0) {
    return;
  }
  state.tags.forEach((tag) => {
    const button = document.createElement("button");
    button.type = "button";
    button.className = `tag-chip${state.activeTag === tag ? " active" : ""}`;
    button.textContent = `# ${tag}`;
    button.addEventListener("click", () => {
      state.activeTag = state.activeTag === tag ? "" : tag;
      renderTags();
      loadPrompts();
    });
    elements.tagFilters.appendChild(button);
  });
}

function renderPrompts() {
  elements.resultCount.textContent = `${state.prompts.length} 条`;
  elements.promptList.innerHTML = "";

  if (state.prompts.length === 0) {
    elements.promptList.innerHTML = `<div class="empty">没有匹配的提示词</div>`;
    return;
  }

  state.prompts.forEach((prompt) => {
    const card = document.createElement("article");
    card.className = "prompt-card";
    const tags = prompt.tags?.map((tag) => `<span># ${escapeHtml(tag)}</span>`).join("") || "";
    card.innerHTML = `
      <header>
        <div>
          <h3 class="prompt-title">${escapeHtml(prompt.title)}</h3>
          <div class="prompt-meta">
            <span>${escapeHtml(prompt.category || "未分类")}</span>
            ${tags}
          </div>
        </div>
        <span class="copy-state">已复制</span>
      </header>
      <p class="prompt-content">${escapeHtml(prompt.content)}</p>
      <div class="card-actions">
        <button class="ghost-btn copy-btn" type="button">复制</button>
        <button class="ghost-btn edit-btn" type="button">编辑</button>
        <button class="danger-btn delete-btn" type="button">删除</button>
      </div>
    `;
    card.querySelector(".copy-btn").addEventListener("click", () => copyPrompt(prompt, card));
    card.querySelector(".edit-btn").addEventListener("click", () => editPrompt(prompt));
    card.querySelector(".delete-btn").addEventListener("click", () => deletePrompt(prompt.id));
    card.addEventListener("dblclick", () => copyPrompt(prompt, card));
    elements.promptList.appendChild(card);
  });
}

function renderCategoryManager() {
  elements.categoryList.innerHTML = "";
  if (state.categories.length === 0) {
    elements.categoryList.innerHTML = `<div class="empty">暂无分类</div>`;
    return;
  }
  state.categories.forEach((category) => {
    const item = document.createElement("div");
    item.className = "category-item";
    item.innerHTML = `
      <span>${escapeHtml(category)}</span>
      <button class="ghost-btn rename-btn" type="button">改名</button>
      <button class="danger-btn remove-btn" type="button">删除</button>
    `;
    item.querySelector(".rename-btn").addEventListener("click", () => renameCategory(category));
    item.querySelector(".remove-btn").addEventListener("click", () => deleteCategory(category));
    elements.categoryList.appendChild(item);
  });
}

function editPrompt(prompt) {
  state.editingId = prompt.id;
  elements.promptId.value = prompt.id;
  elements.formTitle.textContent = "编辑提示词";
  elements.titleInput.value = prompt.title;
  elements.contentInput.value = prompt.content;
  elements.categoryInput.value = prompt.category || "";
  elements.tagsInput.value = (prompt.tags || []).join(", ");
  elements.titleInput.focus();
}

function resetForm() {
  state.editingId = null;
  elements.promptForm.reset();
  elements.promptId.value = "";
  elements.formTitle.textContent = "新增提示词";
}

async function savePrompt(event) {
  event.preventDefault();
  const payload = {
    title: elements.titleInput.value.trim(),
    content: elements.contentInput.value.trim(),
    category: elements.categoryInput.value.trim(),
    tags: splitTags(elements.tagsInput.value)
  };
  const id = elements.promptId.value;
  const path = id ? `/api/prompts/${id}` : "/api/prompts";
  const method = id ? "PUT" : "POST";
  try {
    await request(path, {
      method,
      body: JSON.stringify(payload)
    });
    showToast(id ? "已更新提示词" : "已新增提示词");
    resetForm();
    await loadOptions();
    await loadPrompts();
  } catch (error) {
    showToast(error.message);
  }
}

async function deletePrompt(id) {
  if (!confirm("确定删除这条提示词吗？")) {
    return;
  }
  try {
    await request(`/api/prompts/${id}`, { method: "DELETE" });
    showToast("已删除提示词");
    await loadOptions();
    await loadPrompts();
  } catch (error) {
    showToast(error.message);
  }
}

async function copyPrompt(prompt, card) {
  try {
    await navigator.clipboard.writeText(prompt.content);
    const stateEl = card.querySelector(".copy-state");
    stateEl.classList.add("show");
    window.setTimeout(() => stateEl.classList.remove("show"), 1200);
    showToast("提示词已复制");
  } catch (error) {
    showToast("复制失败，请手动选择内容");
  }
}

async function addCategory(event) {
  event.preventDefault();
  const name = elements.categoryNameInput.value.trim();
  if (!name) {
    return;
  }
  try {
    await request("/api/categories", {
      method: "POST",
      body: JSON.stringify({ name })
    });
    elements.categoryNameInput.value = "";
    showToast("已添加分类");
    await loadOptions();
  } catch (error) {
    showToast(error.message);
  }
}

async function renameCategory(category) {
  const name = prompt("请输入新的分类名称", category);
  if (!name || name.trim() === category) {
    return;
  }
  try {
    await request(`/api/categories/${encodeURIComponent(category)}`, {
      method: "PUT",
      body: JSON.stringify({ name: name.trim() })
    });
    showToast("分类已改名");
    await loadOptions();
    await loadPrompts();
  } catch (error) {
    showToast(error.message);
  }
}

async function deleteCategory(category) {
  if (!confirm(`确定删除分类“${category}”吗？该分类下的提示词会变为未分类。`)) {
    return;
  }
  try {
    await request(`/api/categories/${encodeURIComponent(category)}`, { method: "DELETE" });
    if (elements.categoryFilter.value === category) {
      elements.categoryFilter.value = "";
    }
    showToast("分类已删除");
    await loadOptions();
    await loadPrompts();
  } catch (error) {
    showToast(error.message);
  }
}

function splitTags(value) {
  return value
    .split(/[,，]/)
    .map((tag) => tag.trim())
    .filter(Boolean);
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function debounce(fn, delay = 250) {
  let timer;
  return (...args) => {
    window.clearTimeout(timer);
    timer = window.setTimeout(() => fn(...args), delay);
  };
}

async function init() {
  setNetworkStatus();
  await loadOptions();
  await loadPrompts();
  if ("serviceWorker" in navigator) {
    navigator.serviceWorker.register("/sw.js").catch(() => {});
  }
}

elements.keywordInput.addEventListener("input", debounce(loadPrompts));
elements.categoryFilter.addEventListener("change", loadPrompts);
elements.clearFiltersBtn.addEventListener("click", () => {
  elements.keywordInput.value = "";
  elements.categoryFilter.value = "";
  state.activeTag = "";
  renderTags();
  loadPrompts();
});
elements.promptForm.addEventListener("submit", savePrompt);
elements.resetFormBtn.addEventListener("click", resetForm);
elements.categoryForm.addEventListener("submit", addCategory);
window.addEventListener("online", () => {
  setNetworkStatus();
  loadOptions();
  loadPrompts();
});
window.addEventListener("offline", setNetworkStatus);

init();
