FROM continuumio/miniconda3:23.10.0-1

RUN conda install -y -c conda-forge pythonocc-core=7.7.2 python=3.10 && \
    pip install --no-cache-dir flask==3.0.2 trimesh==4.2.4 numpy==1.26.4 && \
    conda clean -afy

WORKDIR /app
COPY app.py converter.py ./
EXPOSE 5000
CMD ["python", "app.py"]
