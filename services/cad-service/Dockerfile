FROM python:3.10-slim

# pythonocc-core needs some system GL libs (headless OK)
RUN apt-get update && apt-get install -y --no-install-recommends \
      libgl1 libglu1-mesa libxext6 libx11-6 \
    && rm -rf /var/lib/apt/lists/*

RUN pip install --no-cache-dir \
      pythonocc-core==7.7.2 \
      flask==3.0.2 \
      trimesh==4.2.4 \
      numpy==1.26.4

WORKDIR /app
COPY app.py converter.py ./
EXPOSE 5000
CMD ["python", "app.py"]
