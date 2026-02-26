import io
import logging
from flask import Flask, request, send_file, jsonify
from converter import step_to_glb

logging.basicConfig(level=logging.INFO)
log = logging.getLogger(__name__)

app = Flask(__name__)
app.config['MAX_CONTENT_LENGTH'] = 500 * 1024 * 1024  # 500 MB


@app.route('/health')
def health():
    return jsonify({'status': 'ok'})


@app.route('/convert/step-to-glb', methods=['POST'])
def convert():
    if 'file' not in request.files:
        return jsonify({'error': 'No file provided'}), 400

    file = request.files['file']
    if not file.filename:
        return jsonify({'error': 'Empty filename'}), 400

    step_bytes = file.read()
    if not step_bytes:
        return jsonify({'error': 'Empty file'}), 400

    log.info(f"Converting {file.filename} ({len(step_bytes)} bytes)")

    try:
        glb_bytes = step_to_glb(step_bytes)
        log.info(f"Conversion done: {len(glb_bytes)} bytes GLB")
        return send_file(
            io.BytesIO(glb_bytes),
            mimetype='model/gltf-binary',
            as_attachment=True,
            download_name='model.glb'
        )
    except Exception as e:
        log.error(f"Conversion failed: {e}", exc_info=True)
        return jsonify({'error': str(e)}), 422


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
