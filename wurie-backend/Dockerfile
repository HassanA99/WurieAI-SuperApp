FROM python:3.12-slim

WORKDIR /code

ENV PYTHONDONTWRITEBYTECODE=1 \
	PYTHONUNBUFFERED=1 \
	PORT=10000

COPY requirements.txt /code/requirements.txt

RUN pip install --no-cache-dir --upgrade pip uv && \
	uv pip install --system --no-cache-dir -r /code/requirements.txt

COPY app /code/app

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "10000"]
