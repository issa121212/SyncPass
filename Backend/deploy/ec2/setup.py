import argparse,os,pathlib,secrets,subprocess,pwd

parser=argparse.ArgumentParser()
parser.add_argument('--issuer',required=True)
parser.add_argument('--pool',required=True)
parser.add_argument('--client',required=True)
args=parser.parse_args()
assert os.geteuid()==0, 'Run with sudo'
def run(*argv,input=None):
    return subprocess.run(argv,input=input,text=True,stdout=subprocess.PIPE,stderr=subprocess.PIPE,check=True).stdout
def sql(statement):
    return run('runuser','-u','postgres','--','psql','-v','ON_ERROR_STOP=1','-d','postgres','-tAc',statement)

data=pathlib.Path('/var/lib/pgsql/data')
if not (data/'PG_VERSION').exists():
    run('postgresql-setup','--initdb')
run('systemctl','enable','--now','postgresql')
sql("ALTER SYSTEM SET listen_addresses = 'localhost'")
sql("ALTER SYSTEM SET shared_buffers = '64MB'")
sql("ALTER SYSTEM SET max_connections = '30'")
sql("ALTER SYSTEM SET password_encryption = 'scram-sha-256'")
run('systemctl','restart','postgresql')
config=pathlib.Path('/etc/syncpass')
config.mkdir(mode=0o700,exist_ok=True)
env=config/'backend.env'
if env.exists():
    raise SystemExit('Existing backend.env preserved. Setup will not replace deployment credentials.')
password=secrets.token_urlsafe(36)
if sql("SELECT 1 FROM pg_roles WHERE rolname='syncpass'").strip():
    raise SystemExit('Existing syncpass role preserved; inspect setup state before retrying.')
try:
    sql("CREATE ROLE syncpass LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD '"+password+"'")
    run('runuser','-u','postgres','--','createdb','-O','syncpass','syncpass')
except subprocess.CalledProcessError:
    raise SystemExit('Database initialization failed; credentials were not printed.')
hba=data/'pg_hba.conf'
existing=hba.read_text()
hba.write_text('host syncpass syncpass 127.0.0.1/32 scram-sha-256\nhost syncpass syncpass ::1/128 scram-sha-256\n'+existing)
run('systemctl','reload','postgresql')
try:pwd.getpwnam('syncpass')
except KeyError:run('useradd','--system','--home-dir','/opt/syncpass','--shell','/sbin/nologin','syncpass')
pathlib.Path('/opt/syncpass').mkdir(mode=0o755,exist_ok=True)
contents={
    'SPRING_PROFILES_ACTIVE':'prod','SERVER_PORT':'8080','SERVER_ADDRESS':'127.0.0.1',
    'DATABASE_URL':'jdbc:postgresql://127.0.0.1:5432/syncpass','DATABASE_USER':'syncpass',
    'DATABASE_PASSWORD':password,'AWS_REGION':'us-east-1','AWS_COGNITO_USER_POOL_ID':args.pool,
    'AWS_COGNITO_CLIENT_ID':args.client,'COGNITO_ISSUER':args.issuer,
    'SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE':'5',
    'SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE':'1',
}
descriptor=os.open(env,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600)
with os.fdopen(descriptor,'w') as stream:
    stream.write(''.join(key+'='+value+'\n' for key,value in contents.items()))
unit=pathlib.Path('/etc/systemd/system/syncpass.service')
unit.write_text('''[Unit]
Description=SyncPass backend
After=network-online.target postgresql.service
Wants=network-online.target
Requires=postgresql.service
StartLimitIntervalSec=120
StartLimitBurst=4

[Service]
Type=simple
User=syncpass
Group=syncpass
WorkingDirectory=/opt/syncpass
EnvironmentFile=/etc/syncpass/backend.env
ExecStart=/usr/bin/java -Xms64m -Xmx256m -XX:MaxMetaspaceSize=160m -XX:ReservedCodeCacheSize=48m -XX:+ExitOnOutOfMemoryError -jar /opt/syncpass/backend.jar
Restart=on-failure
RestartSec=5
TimeoutStopSec=40
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ProtectHome=true
MemoryHigh=512M
MemoryMax=640M
UMask=0077

[Install]
WantedBy=multi-user.target
''')
swap=pathlib.Path('/var/swap/syncpass.swap')
if not swap.exists():
    swap.parent.mkdir(mode=0o700,exist_ok=True)
    run('fallocate','-l','1G',str(swap))
    swap.chmod(0o600)
    run('mkswap',str(swap))
    run('swapon',str(swap))
    fstab=pathlib.Path('/etc/fstab')
    if str(swap) not in fstab.read_text():
        with fstab.open('a') as stream:stream.write('\n'+str(swap)+' none swap sw 0 0\n')
run('systemctl','daemon-reload')
print('PostgreSQL local configurado; base y usuario SyncPass creados; credenciales privadas guardadas solo en EC2.')
print('Servicio systemd preparado y swap local de 1 GiB configurado. Falta instalar el JAR e iniciar SyncPass.')
