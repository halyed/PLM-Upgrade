# Use the official pre-built pythonocc image — avoids heavy conda install on VPS
FROM tpaviot/pythonocc-core:7.7.2

RUN pip install --no-cache-dir flask==3.0.2 trimesh==4.2.4 numpy==1.26.4

WORKDIR /app
COPY app.py converter.py ./
EXPOSE 5000
CMD ["python", "app.py"]
