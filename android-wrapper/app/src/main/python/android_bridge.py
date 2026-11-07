"""Thin Android bridge for the unchanged AIRA Python source archive."""
from __future__ import annotations

import threading
from pathlib import Path

from core.config import load_settings
from core.main import build_app
from core.router import Command, parse
from security.secrets import get_api_key, redact, store_api_key

_lock = threading.RLock()
_output: list[str] = []
_app = None
_root: Path | None = None
_key_path: Path | None = None


def initialize(app_files_dir: str) -> str:
    """Initialize AIRA data under Android's private app files directory."""
    global _app, _root, _key_path
    with _lock:
        _root = Path(app_files_dir) / "aira-agent"
        (_root / "config").mkdir(parents=True, exist_ok=True)
        (_root / "data").mkdir(parents=True, exist_ok=True)
        (_root / "workspace").mkdir(parents=True, exist_ok=True)
        _key_path = _root / "data" / "api_key"
        import os
        os.environ["AIRA_KEY_FILE"] = str(_key_path)
        settings = load_settings(root=_root)
        _output.clear()
        # The CLI's approval callback is deliberately default-deny in this wrapper.
        _app = build_app(
            settings,
            input_fn=lambda _prompt="": "no",
            output_fn=_output.append,
        )
        return "ready"


def has_api_key() -> bool:
    with _lock:
        return bool(get_api_key(key_file=_key_path))


def save_api_key(key: str) -> str:
    with _lock:
        if _key_path is None:
            raise RuntimeError("AIRA is not initialized")
        store_api_key(str(key), key_file=_key_path, overwrite=True)
        return "saved"


def clear_api_key() -> str:
    with _lock:
        if _key_path is None:
            raise RuntimeError("AIRA is not initialized")
        try:
            _key_path.unlink()
        except FileNotFoundError:
            pass
        return "removed"


def send_message(text: str) -> str:
    """Run one CLI-compatible AIRA turn and return captured output."""
    with _lock:
        if _app is None:
            raise RuntimeError("AIRA is not initialized")
        line = str(text).strip()
        if not line:
            return ""
        command = parse(line)
        if isinstance(command, Command) and command.name in {"write", "overwrite"}:
            return (
                "এই Android wrapper-এ terminal-এর multiline /write ও /overwrite command বন্ধ। "
                "কোনো ফাইল পরিবর্তন করা হয়নি।"
            )
        _output.clear()
        try:
            _app.handle(line)
        except Exception as exc:
            # Avoid exposing credentials if a lower-level error includes request data.
            return "AIRA error: " + redact(f"{type(exc).__name__}: {exc}")[:500]
        return "\n".join(str(item) for item in _output).strip()
