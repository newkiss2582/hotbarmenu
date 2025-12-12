วิธีใช้งาน Plugin: SlotHotKeys
ฟีเจอร์หลัก
1) Slot 0 (ช่องเลข 1 ในเกม)

เป็นช่อง “อาวุธ”

ใส่ได้เฉพาะ MMOItems ที่ type อยู่ใน list settings.weapon-types (เช่น SWORD/DAGGER/STAFF ฯลฯ)

ถ้าเอาของอื่นมาวาง/สลับ/ยัดเข้ามา → จะถูกบล็อกหรือถูกย้ายออก

2) Slot 1–2 (ช่องเลข 2–3 ในเกม)

เป็นช่อง “ยาอัตโนมัติ”

ใส่ได้เฉพาะ MMOItems type ตาม settings.potion-types (เช่น BPOTION)

ถ้า HP ต่ำกว่าเปอร์เซ็นต์ที่ตั้งไว้ → จะกินอัตโนมัติ

ลำดับการใช้: ช่อง 1 ก่อน → ถ้าไม่มีค่อยช่อง 2

กินสำเร็จแล้ว จำนวนลดลง 1 ต่อครั้ง (ถ้าหมดจะหายจากช่อง)

3) Slot 3–8 (ช่องเลข 4–9 ในเกม) = เมนู

ระบบจะ “ใส่ไอเทมเมนู” ให้เองตาม config (material / custom_model_data / name / lore)

ห้ามย้าย / ห้ามทิ้ง / ห้ามวางทับ

เวลา “สลับไปถือช่องนั้น” จะ รันคำสั่ง ที่กำหนดใน config (menus.<slot>.command) โดยรันผ่าน Console

รองรับ %player% เพื่อแทนชื่อผู้เล่น

4) กันของดรอปไปเข้า Slot 0/1/2

ตอนเก็บของจากพื้น ถ้ามันเผลอเข้า 0/1/2 และไม่ใช่ของที่อนุญาต → plugin จะย้ายออกอัตโนมัติไปช่องอื่น (ถ้าเต็มจะดรอปลงพื้น)

คำสั่ง
/slothotkeys reload

รีโหลด config.yml (ต้องมี permission slothotkeys.admin)

/slothotkeys potion

เปิด GUI ตั้งค่า Auto Potion (รายคน)

ปุ่ม:

Dye = เปิด/ปิดการใช้งาน Auto Potion

Wool (แดง/เขียว) = เลือกเปอร์เซ็นต์ที่ต้องการให้กินยา (10/20/30/50/60/70/80/90)

วิธีตั้งค่า (Config)

ไฟล์: plugins/SlotHotKeys/config.yml

weapon-types

กำหนด MMOItems type ที่ใส่ได้ใน slot 0:

settings:
  weapon-types:
    - SWORD
    - DAGGER
    - HAMMER
    - STAFF
    - SPEAR

potion-types

กำหนด MMOItems type ที่ใส่ได้ใน slot 1–2:

settings:
  potion-types:
    - BPOTION

menu items (slot 3–8)

กำหนด item เมนู + คำสั่ง:

menus:
  "4":
    material: COMPASS
    custom_model_data: 1002
    name: "&bWarp Menu"
    lore:
      - "&7Open warp GUI"
    command: "warp gui %player%"

การทำงาน Auto Potion (เชิงระบบ)

Scheduler จะรันทุก check-interval-ticks

สำหรับผู้เล่นแต่ละคน:

เช็คว่าเปิด Auto Potion อยู่ไหม (จาก GUI)

อ่านค่า threshold จาก GUI (เช่น 0.50)

ดึง HP จาก MMOCore PlaceholderParser (mmocore_health / mmocore_max_health)

ถ้า HP% < threshold:

ถ้าไม่ติดคูลดาวน์ → พยายามกินจาก Slot 1 ก่อน แล้วค่อย Slot 2

ถ้ากินสำเร็จ → ลดจำนวนยา 1 และเริ่มคูลดาวน์

สิ่งที่ต้องมี (Dependencies)

Paper/Spigot 1.20+

MMOItems

MMOCore

MythicLib

วิธีติดตั้ง

Build jar จาก Maven (mvn clean package)

นำไฟล์ .jar ไปไว้ในโฟลเดอร์ plugins/

รีสตาร์ทเซิร์ฟเวอร์

แก้ config ตามต้องการ

/slothotkeys reload

วิธีลง GitHub (ทำ Repo + Push)
1) สร้าง Repo บน GitHub

เข้า GitHub → New repository

ตั้งชื่อ เช่น SlotHotKeys

เลือก Public/Private ได้ตามสะดวก

Create repository

2) Push โปรเจคขึ้น GitHub (ในเครื่อง)

เปิด Terminal / PowerShell ในโฟลเดอร์โปรเจคเดียวกับ pom.xml

git init
git add .
git commit -m "Initial commit - SlotHotKeys"
git branch -M main
git remote add origin https://github.com/<USER>/<REPO>.git
git push -u origin main


เปลี่ยน <USER> และ <REPO> ให้ตรงของมึง

3) แนะนำใส่ .gitignore สำหรับ Maven/IDE

สร้างไฟล์ .gitignore ที่ root โปรเจค:

/target/
/.idea/
/*.iml
.classpath
.project
.settings/
.DS_Store
*.log

โครงสร้างโปรเจคแนะนำบน GitHub

ใส่ README.md (ใช้เนื้อหาด้านบนได้เลย)

ใส่ LICENSE (ถ้าจะเปิด public)

ใส่ releases (ปล่อย jar ใน GitHub Releases)
