let questions = [];

function shuffle(array) {
  for (let i = array.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [array[i], array[j]] = [array[j], array[i]];
  }
}

async function loadQuestions() {
  const response = await fetch('/questions');
  questions = await response.json();

  const container = document.getElementById('questions');
  container.innerHTML = '';

  questions.forEach((q, index) => {
    const div = document.createElement('div');
    div.className = 'question';
    div.innerHTML = `<p><strong>Question ${index + 1}:</strong> ${q.question}</p>`;

    const options = [...q.options];

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