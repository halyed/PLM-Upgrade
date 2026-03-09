FROM mambaorg/micromamba:1.5.8

# Install pythonocc-core + python in base env
ARG MAMBA_DOCKERFILE_ACTIVATE=1
USER root

RUN micromamba install -y -n base -c conda-forge \
      python=3.10 pythonocc-core=7.7.2 && \
    micromamba clean -afy

RUN pip install --no-cache-dir flask==3.0.2 trimesh==4.2.4 numpy==1.26.4

WORKDIR /app
COPY app.py converter.py ./
EXPOSE 5000

ENTRYPOINT ["micromamba", "run", "-n", "base", "python", "app.py"]
