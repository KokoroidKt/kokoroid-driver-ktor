#!/usr/bin/env python3
import click
import os
import subprocess
from pathlib import Path
from concurrent.futures import ProcessPoolExecutor


def gen_command(fullpath: Path, force: bool, name: str):
    command = [
        "uv",
        "run",
        "reuse",
        "annotate",
        str(fullpath),
        "--copyright=Kokoroid Contributors",
        "--license=LGPL-2.1",
    ]
    if name:
        command.append(f'--contributor={name}')
    if not force:
        command.append("--skip-existing")
    return str(fullpath), command


def run_single_command(command_info, debug):
    path, cmd = command_info
    if debug:
        print(f"adding header for {path} ({" ".join(cmd)})")
    result = subprocess.run(cmd, capture_output=True, text=True)
    return path, result.stdout, result.stderr, result.returncode


def commands(force: bool, name: str):
    modules = [
        Path("core"),
        Path("api")

    ]
    for module in modules:
        for item in (module / "src" / "main", module / "src" / "test"):
            if not item.exists():
                continue
            for file in item.rglob("*.kt"):
                fullpath = file.resolve()
                yield gen_command(fullpath, force, name)


@click.command()
@click.option("-n", "--name", help="contributor name")
@click.option("-f", "--force", is_flag=True, help="force add copyright headers without confirmation")
@click.option("--debug", is_flag=True, help="enable debug mode")
@click.option("-p", "--processes", default=os.cpu_count(), help="number of parallel processes")
def main(name, force=False, debug=False, processes=None):
    if name is None:
        if not click.confirm(
                f'No contributor name was provided. Only the default copyright header will be added. This is HARD to change later. Continue?'):
            click.echo("Aborted")
            click.echo("use `uv run ./scripts/add-copyright.py --name <YOUR_NAME>` to set your name!")
            raise click.Abort()
    elif not click.confirm(
            f'You are about to add contributor name "{name}" to copyright headers. This is hard to change later. Continue?'):
        click.echo("Aborted")
        click.echo("use `uv run ./scripts/add-copyright.py --name <YOUR_NAME>` to set correct name!")
        raise click.Abort()
    if force and not click.confirm(
            f'FORCE mode is enabled. This will overwrite ALL existing copyright headers. ARE YOU SURE?'):
        click.echo("Aborted")
        click.echo("remove -f or --force flag to disable it!")
        raise click.Abort()

    click.echo(f"Adding copyright headers {f"for {name}" if name else ""} (using {processes} processes)......")
    
    all_commands = list(commands(force, name))
    success = []
    failed = []

    with ProcessPoolExecutor(max_workers=processes) as executor:
        futures = [executor.submit(run_single_command, cmd, debug) for cmd in all_commands]
        for future in futures:
            path, stdout, stderr, returncode = future.result()
            click.echo(stdout, nl=False)
            if returncode != 0:
                click.echo("ERROR:")
                if stderr:
                    click.echo(stderr)
                failed.append(path)
            else:
                success.append(path)

    if failed:
        click.echo("Failed:")
        for item in failed:
            click.echo(item)
    elif debug:
        click.echo("Successed:")
        click.echo(success)
    click.echo(f"Success: {len(success)}")
    click.echo(f"Failed: {len(failed)}")


if __name__ == "__main__":
    main()
