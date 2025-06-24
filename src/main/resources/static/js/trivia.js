let questions = [];

function shuffle(array) {
  for (let i = array.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [array[i], array[j]] = [array[j], array[i]];
  }
}

async function fetchQuestionsWithRetry(retries = 3, delay = 5000) {
  for (let attempt = 1; attempt <= retries; attempt++) {
    try {
      const response = await fetch('/questions');
      if (!response.ok) throw new Error(`HTTP error ${response.status}`);

      const data = await response.json();
      if (!Array.isArray(data) || data.length === 0) {
        throw new Error('No questions returned');
      }

      return data; // success!
    } catch (err) {
      console.warn(`Attempt ${attempt} failed: ${err.message}`);
      if (attempt < retries) {
        await new Promise(resolve => setTimeout(resolve, delay));
      } else {
        throw new Error('Max retries reached');
      }
    }
  }
}

async function loadQuestions() {
  const container = document.getElementById('questions');
  const loadingIndicator = document.getElementById('loading-indicator');

  loadingIndicator.classList.remove('hidden');
  container.innerHTML = ''; // clear previous questions or messages

  try {
    questions = await fetchQuestionsWithRetry();

    loadingIndicator.classList.add('hidden');

    questions.forEach((q, index) => {
      const div = document.createElement('div');
      div.className = 'question';
      div.innerHTML = `<p><strong>Question ${index + 1}:</strong> ${q.question}</p>`;

      const options = [...q.options];
      shuffle(options);

      options.forEach(answer => {
        const inputId = `q${q.id}-${answer}`;
        div.innerHTML += `
          <div class="option">
            <input type="radio" id="${inputId}" name="${q.id}" value="${answer}" />
            <label for="${inputId}">${answer}</label>
          </div>
        `;
      });

      container.appendChild(div);
    });

  } catch (error) {
    spinner.classList.add('hidden');
    container.innerHTML = `<p style="color: red;">❌ Failed to load questions. Please try again later.</p>`;
    console.error(error.message);
  }
}

async function submitAnswers() {
  const userAnswers = {};

  questions.forEach(q => {
    const selected = document.querySelector(`input[name="${q.id}"]:checked`);
    if (selected) {
      userAnswers[q.id] = selected.value;
    }
  });

  const response = await fetch('/checkanswers', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ userAnswers })
  });

  const resultData = await response.json();
  const resultsDiv = document.getElementById('results');
  resultsDiv.innerHTML = '';

  questions.forEach((q, index) => {
    const isCorrect = resultData.results[q.id];
    const p = document.createElement('p');
    p.textContent = `Question ${index + 1}: ${isCorrect ? '✅ Correct' : '❌ Wrong'}`;
    p.className = isCorrect ? 'correct' : 'incorrect';
    resultsDiv.appendChild(p);
  });
}

document.getElementById('submit-btn').addEventListener('click', submitAnswers);
window.addEventListener('load', loadQuestions);