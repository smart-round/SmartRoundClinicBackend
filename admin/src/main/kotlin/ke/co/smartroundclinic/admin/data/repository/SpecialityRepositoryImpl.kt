package ke.co.smartroundclinic.admin.data.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.admin.data.entity.ServiceCategoryEntity
import ke.co.smartroundclinic.admin.data.entity.ServiceTierEntity
import ke.co.smartroundclinic.admin.data.entity.SpecialityEntity
import ke.co.smartroundclinic.admin.data.entity.SubspecialtyEntity
import ke.co.smartroundclinic.admin.domain.repository.SpecialityRepository
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bson.conversions.Bson

class SpecialityRepositoryImpl(
    private val client: MongoClient,
    database: MongoDatabase,
) : SpecialityRepository {

    private val specialities = database.getCollection<SpecialityEntity>(MongoDBConstants.ADMIN_SPECIALITIES)
    private val subSpecialities = database.getCollection<SubspecialtyEntity>(MongoDBConstants.ADMIN_SUB_SPECIALITIES)
    private val serviceTiers = database.getCollection<ServiceTierEntity>(MongoDBConstants.ADMIN_SERVICE_TIERS)
    private val serviceCategories = database.getCollection<ServiceCategoryEntity>(MongoDBConstants.ADMIN_SERVICE_CATEGORIES)

    private fun seed() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (specialities.countDocuments() > 0) return@launch

                val seedData = listOf(
                    SeedEntry(
                        speciality = SpecialityEntity(title = "General Practice", description = "First point of contact for all health concerns. General Practitioners diagnose and treat a wide range of conditions, provide preventive care, and refer patients to specialists when needed.", color = "#2563EB", iconUrl = "https://unpkg.com/lucide-static@latest/icons/stethoscope.svg"),
                        subspecialties = listOf(
                            "Family Medicine" to ("Comprehensive healthcare for individuals and families across all ages, genders, and diseases — from childhood through old age." to "https://unpkg.com/lucide-static@latest/icons/house.svg"),
                            "Internal Medicine" to ("Diagnosis and non-surgical treatment of complex adult diseases, often serving as the bridge between general care and specialist referral." to "https://unpkg.com/lucide-static@latest/icons/clipboard-list.svg"),
                            "Geriatrics" to ("Specialized care for older adults, focusing on the prevention, diagnosis, and treatment of diseases and conditions that occur in later life." to "https://unpkg.com/lucide-static@latest/icons/user-round-check.svg"),
                            "Travel Medicine" to ("Prevention and management of health problems in travelers, including vaccinations, prophylaxis, and post-travel illness assessment." to "https://unpkg.com/lucide-static@latest/icons/globe.svg"),
                        )
                    ),
                    SeedEntry(
                        speciality = SpecialityEntity(title = "Obstetrics & Gynecology", description = "Comprehensive care of the female reproductive system spanning all life stages — from adolescence through menopause, including pregnancy, childbirth, and gynecological health.", color = "#DB2777", iconUrl = "https://unpkg.com/lucide-static@latest/icons/baby.svg"),
                        subspecialties = listOf(
                            "General OB/GYN" to ("Routine and preventive women's health care including Pap smears, well-woman exams, contraception counseling, prenatal care, and routine deliveries." to "https://unpkg.com/lucide-static@latest/icons/circle-heart.svg"),
                            "Maternal-Fetal Medicine" to ("Specialized care for high-risk pregnancies involving conditions such as preeclampsia, gestational diabetes, preterm labor, fetal anomalies, or serious maternal illness." to "https://unpkg.com/lucide-static@latest/icons/heart-handshake.svg"),
                            "Reproductive Endocrinology & Infertility" to ("Evaluation and treatment of hormonal disorders and infertility. Services include IVF, IUI, egg freezing, and management of conditions like PCOS and endometriosis." to "https://unpkg.com/lucide-static@latest/icons/dna.svg"),
                            "Gynecologic Oncology" to ("Diagnosis and treatment of cancers of the female reproductive system including ovarian, cervical, uterine, vulvar, and vaginal cancers, using surgery, chemotherapy, and palliative care." to "https://unpkg.com/lucide-static@latest/icons/microscope.svg"),
                            "Urogynecology / FPMRS" to ("Female Pelvic Medicine and Reconstructive Surgery — treatment of pelvic floor disorders including urinary incontinence, pelvic organ prolapse, and pelvic pain." to "https://unpkg.com/lucide-static@latest/icons/layers.svg"),
                            "Complex Family Planning" to ("Contraception and abortion care for patients with complex medical or surgical conditions who require more nuanced reproductive health management." to "https://unpkg.com/lucide-static@latest/icons/calendar-heart.svg"),
                            "Minimally Invasive Gynecologic Surgery" to ("Advanced laparoscopic and robotic surgical techniques for managing conditions like fibroids, endometriosis, ovarian cysts, and abnormal uterine bleeding." to "https://unpkg.com/lucide-static@latest/icons/zap.svg"),
                            "Pediatric & Adolescent Gynecology" to ("Specialized gynecological care for children and teenagers, addressing puberty concerns, menstrual disorders, and reproductive health education." to "https://unpkg.com/lucide-static@latest/icons/sprout.svg"),
                            "Menopause Medicine" to ("Management of perimenopause and menopause symptoms including hormone therapy, bone health, cardiovascular risk, and sexual health in midlife women." to "https://unpkg.com/lucide-static@latest/icons/sun.svg"),
                        )
                    ),
                    SeedEntry(
                        speciality = SpecialityEntity(title = "Pediatrics", description = "Medical care for infants, children, and adolescents from birth through age 18, covering growth, development, and childhood illnesses.", color = "#7C3AED", iconUrl = "https://unpkg.com/lucide-static@latest/icons/smile.svg"),
                        subspecialties = listOf(
                            "General Pediatrics" to ("Routine well-child visits, vaccinations, growth monitoring, and treatment of common childhood illnesses." to "https://unpkg.com/lucide-static@latest/icons/star.svg"),
                            "Neonatology" to ("Specialized care for newborns, particularly premature or critically ill babies, typically in a Neonatal Intensive Care Unit (NICU)." to "https://unpkg.com/lucide-static@latest/icons/baby.svg"),
                            "Pediatric Cardiology" to ("Diagnosis and management of heart conditions in children, including congenital heart defects and arrhythmias." to "https://unpkg.com/lucide-static@latest/icons/heart-pulse.svg"),
                            "Pediatric Neurology" to ("Treatment of neurological disorders in children including epilepsy, cerebral palsy, headaches, and developmental delays." to "https://unpkg.com/lucide-static@latest/icons/brain.svg"),
                            "Pediatric Oncology" to ("Diagnosis and treatment of cancers in children and adolescents, including leukemia, brain tumors, and lymphomas." to "https://unpkg.com/lucide-static@latest/icons/microscope.svg"),
                            "Pediatric Gastroenterology" to ("Management of digestive and liver disorders in children, such as IBD, celiac disease, and feeding difficulties." to "https://unpkg.com/lucide-static@latest/icons/activity.svg"),
                        )
                    ),
                    SeedEntry(
                        speciality = SpecialityEntity(title = "Cardiology", description = "Diagnosis and treatment of diseases and conditions of the heart and cardiovascular system, from routine heart health to complex interventional procedures.", color = "#DC2626", iconUrl = "https://unpkg.com/lucide-static@latest/icons/heart-pulse.svg"),
                        subspecialties = listOf(
                            "General Cardiology" to ("Management of heart disease, high blood pressure, cholesterol, and cardiac risk factor assessment." to "https://unpkg.com/lucide-static@latest/icons/heart.svg"),
                            "Interventional Cardiology" to ("Catheter-based procedures to treat blocked arteries, including angioplasty and stent placement." to "https://unpkg.com/lucide-static@latest/icons/zap.svg"),
                            "Cardiac Electrophysiology" to ("Diagnosis and treatment of heart rhythm disorders (arrhythmias), including pacemaker implantation and ablation procedures." to "https://unpkg.com/lucide-static@latest/icons/activity.svg"),
                            "Heart Failure & Transplant" to ("Advanced management of heart failure, cardiomyopathies, and evaluation for cardiac transplantation." to "https://unpkg.com/lucide-static@latest/icons/shield.svg"),
                            "Preventive Cardiology" to ("Risk factor management and lifestyle intervention to prevent the onset or progression of cardiovascular disease." to "https://unpkg.com/lucide-static@latest/icons/shield-check.svg"),
                        )
                    ),
                    SeedEntry(
                        speciality = SpecialityEntity(title = "Neurology", description = "Diagnosis and treatment of disorders of the nervous system, including the brain, spinal cord, peripheral nerves, and muscles.", color = "#0891B2", iconUrl = "https://unpkg.com/lucide-static@latest/icons/brain.svg"),
                        subspecialties = listOf(
                            "General Neurology" to ("Evaluation and management of headaches, dizziness, neuropathy, seizures, and other common neurological conditions." to "https://unpkg.com/lucide-static@latest/icons/activity.svg"),
                            "Stroke & Cerebrovascular" to ("Prevention, acute treatment, and rehabilitation following strokes, TIAs, and other brain vascular events." to "https://unpkg.com/lucide-static@latest/icons/zap.svg"),
                            "Epilepsy" to ("Comprehensive care for patients with epilepsy, including EEG monitoring, medication management, and surgical evaluation." to "https://unpkg.com/lucide-static@latest/icons/wave.svg"),
                            "Movement Disorders" to ("Treatment of Parkinson's disease, tremor, dystonia, and other conditions affecting movement and motor control." to "https://unpkg.com/lucide-static@latest/icons/move.svg"),
                            "Neuromuscular Medicine" to ("Diagnosis and management of diseases of the peripheral nerves and muscles, including ALS, muscular dystrophy, and myasthenia gravis." to "https://unpkg.com/lucide-static@latest/icons/layers.svg"),
                            "Neuro-Oncology" to ("Treatment of brain and spinal cord tumors as well as neurological complications of systemic cancers." to "https://unpkg.com/lucide-static@latest/icons/microscope.svg"),
                        )
                    ),
                    SeedEntry(
                        speciality = SpecialityEntity(title = "Dermatology", description = "Medical and cosmetic care for conditions affecting the skin, hair, and nails — from acne and eczema to skin cancer screening and aesthetic procedures.", color = "#D97706", iconUrl = "https://unpkg.com/lucide-static@latest/icons/scan-face.svg"),
                        subspecialties = listOf(
                            "General Dermatology" to ("Diagnosis and treatment of acne, eczema, psoriasis, rashes, infections, and routine skin checks." to "https://unpkg.com/lucide-static@latest/icons/shield.svg"),
                            "Cosmetic Dermatology" to ("Non-surgical aesthetic procedures including Botox, fillers, laser treatments, chemical peels, and skin rejuvenation." to "https://unpkg.com/lucide-static@latest/icons/sparkles.svg"),
                            "Onco-Dermatology" to ("Diagnosis and management of skin cancers including melanoma, basal cell carcinoma, and squamous cell carcinoma." to "https://unpkg.com/lucide-static@latest/icons/microscope.svg"),
                            "Pediatric Dermatology" to ("Skin care for children including birthmarks, atopic dermatitis, genetic skin conditions, and vascular anomalies." to "https://unpkg.com/lucide-static@latest/icons/star.svg"),
                            "Trichology & Hair Disorders" to ("Specialized evaluation and treatment of hair loss (alopecia), scalp conditions, and nail disorders." to "https://unpkg.com/lucide-static@latest/icons/wind.svg"),
                        )
                    ),
                    SeedEntry(
                        speciality = SpecialityEntity(title = "Psychiatry & Mental Health", description = "Assessment, diagnosis, and treatment of mental, emotional, and behavioral disorders using medication, psychotherapy, and a holistic approach to wellbeing.", color = "#7C3AED", iconUrl = "https://unpkg.com/lucide-static@latest/icons/brain-circuit.svg"),
                        subspecialties = listOf(
                            "General Psychiatry" to ("Evaluation and medication management for depression, anxiety, bipolar disorder, schizophrenia, and other mental health conditions." to "https://unpkg.com/lucide-static@latest/icons/activity.svg"),
                            "Child & Adolescent Psychiatry" to ("Mental health diagnosis and treatment for children and teenagers, including ADHD, autism spectrum disorder, and behavioral disorders." to "https://unpkg.com/lucide-static@latest/icons/star.svg"),
                            "Addiction Psychiatry" to ("Evaluation and treatment of substance use disorders including alcohol, opioids, and other drug dependencies." to "https://unpkg.com/lucide-static@latest/icons/shield.svg"),
                            "Geriatric Psychiatry" to ("Mental health care for older adults, including dementia-related behavioral issues, late-life depression, and cognitive decline." to "https://unpkg.com/lucide-static@latest/icons/user-round-check.svg"),
                            "Forensic Psychiatry" to ("Intersection of mental health and the legal system, including competency evaluations and treatment of offenders with psychiatric disorders." to "https://unpkg.com/lucide-static@latest/icons/scale.svg"),
                            "Psychotherapy / Counseling" to ("Talk-based therapies including CBT, DBT, trauma therapy, and couples/family counseling delivered by licensed therapists and psychologists." to "https://unpkg.com/lucide-static@latest/icons/message-circle.svg"),
                        )
                    ),
                    SeedEntry(
                        speciality = SpecialityEntity(title = "Orthopedics", description = "Diagnosis and treatment of injuries and diseases of the musculoskeletal system — bones, joints, ligaments, tendons, muscles, and nerves.", color = "#059669", iconUrl = "https://unpkg.com/lucide-static@latest/icons/bone.svg"),
                        subspecialties = listOf(
                            "General Orthopedics" to ("Assessment and management of fractures, sprains, back pain, and common joint problems." to "https://unpkg.com/lucide-static@latest/icons/shield.svg"),
                            "Sports Medicine" to ("Prevention, diagnosis, and rehabilitation of sports-related injuries including ligament tears, stress fractures, and overuse conditions." to "https://unpkg.com/lucide-static@latest/icons/zap.svg"),
                            "Spine Surgery" to ("Surgical and non-surgical treatment of spine conditions including herniated discs, scoliosis, and spinal stenosis." to "https://unpkg.com/lucide-static@latest/icons/layers.svg"),
                            "Joint Replacement" to ("Evaluation and surgical planning for hip, knee, and shoulder joint replacement in patients with arthritis or injury." to "https://unpkg.com/lucide-static@latest/icons/settings.svg"),
                            "Pediatric Orthopedics" to ("Bone and joint care specific to children including growth plate injuries, limb deformities, and scoliosis in adolescents." to "https://unpkg.com/lucide-static@latest/icons/star.svg"),
                        )
                    ),
                    SeedEntry(
                        speciality = SpecialityEntity(title = "Gastroenterology", description = "Diagnosis and treatment of diseases of the digestive system including the esophagus, stomach, intestines, liver, pancreas, and gallbladder.", color = "#B45309", iconUrl = "https://unpkg.com/lucide-static@latest/icons/activity.svg"),
                        subspecialties = listOf(
                            "General Gastroenterology" to ("Management of acid reflux, IBS, ulcers, diarrhea, constipation, and colonoscopy for colorectal cancer screening." to "https://unpkg.com/lucide-static@latest/icons/shield.svg"),
                            "Hepatology" to ("Specialized care for liver diseases including hepatitis B & C, cirrhosis, fatty liver disease, and liver transplant evaluation." to "https://unpkg.com/lucide-static@latest/icons/droplets.svg"),
                            "Inflammatory Bowel Disease" to ("Diagnosis and long-term management of Crohn's disease and ulcerative colitis using medical and biological therapies." to "https://unpkg.com/lucide-static@latest/icons/flame.svg"),
                            "Advanced Endoscopy" to ("Therapeutic procedures including ERCP for bile duct disease, endoscopic ultrasound, and complex polyp removal." to "https://unpkg.com/lucide-static@latest/icons/eye.svg"),
                        )
                    ),
                    SeedEntry(
                        speciality = SpecialityEntity(title = "Endocrinology", description = "Diagnosis and management of hormone-related conditions and metabolic disorders affecting glands such as the thyroid, pancreas, adrenal glands, and pituitary.", color = "#0D9488", iconUrl = "https://unpkg.com/lucide-static@latest/icons/flask-conical.svg"),
                        subspecialties = listOf(
                            "Diabetes & Metabolism" to ("Comprehensive management of Type 1, Type 2, and gestational diabetes including insulin therapy, CGM, and lifestyle coaching." to "https://unpkg.com/lucide-static@latest/icons/droplets.svg"),
                            "Thyroid & Parathyroid" to ("Evaluation and treatment of hypothyroidism, hyperthyroidism, thyroid nodules, goiter, and parathyroid disorders." to "https://unpkg.com/lucide-static@latest/icons/activity.svg"),
                            "Adrenal & Pituitary Disorders" to ("Management of adrenal insufficiency, Cushing's syndrome, acromegaly, and other pituitary hormone disorders." to "https://unpkg.com/lucide-static@latest/icons/brain.svg"),
                            "Bone & Mineral Metabolism" to ("Diagnosis and treatment of osteoporosis, vitamin D deficiency, and disorders of calcium and bone metabolism." to "https://unpkg.com/lucide-static@latest/icons/bone.svg"),
                            "Obesity Medicine" to ("Medical management of obesity including lifestyle counseling, pharmacotherapy, and pre/post-bariatric surgical care." to "https://unpkg.com/lucide-static@latest/icons/heart.svg"),
                        )
                    ),
                    SeedEntry(
                        speciality = SpecialityEntity(title = "Pulmonology", description = "Diagnosis and treatment of diseases of the lungs and respiratory system, including both chronic conditions and acute respiratory illness.", color = "#2563EB", iconUrl = "https://unpkg.com/lucide-static@latest/icons/wind.svg"),
                        subspecialties = listOf(
                            "General Pulmonology" to ("Management of asthma, COPD, chronic cough, and other common respiratory conditions." to "https://unpkg.com/lucide-static@latest/icons/activity.svg"),
                            "Sleep Medicine" to ("Diagnosis and treatment of sleep disorders including obstructive sleep apnea, insomnia, narcolepsy, and restless legs syndrome." to "https://unpkg.com/lucide-static@latest/icons/moon.svg"),
                            "Interventional Pulmonology" to ("Bronchoscopy, thoracentesis, and other procedures for diagnosis and treatment of lung and airway disease." to "https://unpkg.com/lucide-static@latest/icons/zap.svg"),
                            "Critical Care Medicine" to ("Intensive care management of patients with severe respiratory failure, sepsis, and multi-organ dysfunction." to "https://unpkg.com/lucide-static@latest/icons/alert-triangle.svg"),
                        )
                    ),
                    SeedEntry(
                        speciality = SpecialityEntity(title = "Nephrology", description = "Specialty focused on kidney health — from chronic kidney disease and hypertension management to dialysis and transplant care.", color = "#0891B2", iconUrl = "https://unpkg.com/lucide-static@latest/icons/droplets.svg"),
                        subspecialties = listOf(
                            "General Nephrology" to ("Evaluation of kidney function, management of CKD, proteinuria, hematuria, and kidney stones." to "https://unpkg.com/lucide-static@latest/icons/shield.svg"),
                            "Dialysis Medicine" to ("Management of patients on hemodialysis or peritoneal dialysis for end-stage renal disease." to "https://unpkg.com/lucide-static@latest/icons/activity.svg"),
                            "Transplant Nephrology" to ("Pre- and post-kidney transplant care including immunosuppression management and rejection monitoring." to "https://unpkg.com/lucide-static@latest/icons/heart.svg"),
                            "Hypertension Specialist" to ("Advanced management of resistant or secondary hypertension and its impact on the kidneys and cardiovascular system." to "https://unpkg.com/lucide-static@latest/icons/trending-up.svg"),
                        )
                    ),
                    SeedEntry(
                        speciality = SpecialityEntity(title = "Oncology", description = "Comprehensive cancer care covering diagnosis, staging, treatment planning, and palliative support for all cancer types.", color = "#9333EA", iconUrl = "https://unpkg.com/lucide-static@latest/icons/microscope.svg"),
                        subspecialties = listOf(
                            "Medical Oncology" to ("Non-surgical cancer treatment using chemotherapy, immunotherapy, targeted therapy, and hormone therapy." to "https://unpkg.com/lucide-static@latest/icons/flask-conical.svg"),
                            "Radiation Oncology" to ("Use of radiation therapy to treat and manage various cancers, including stereotactic radiosurgery and brachytherapy." to "https://unpkg.com/lucide-static@latest/icons/zap.svg"),
                            "Surgical Oncology" to ("Surgical removal of tumors and cancer tissue, including biopsies, resections, and debulking procedures." to "https://unpkg.com/lucide-static@latest/icons/scissors.svg"),
                            "Palliative & Supportive Care" to ("Symptom management, pain relief, and quality-of-life support for patients with serious or terminal illness." to "https://unpkg.com/lucide-static@latest/icons/heart.svg"),
                            "Hematologic Oncology" to ("Treatment of blood cancers including leukemia, lymphoma, multiple myeloma, and stem cell transplantation." to "https://unpkg.com/lucide-static@latest/icons/droplets.svg"),
                        )
                    ),
                    SeedEntry(
                        speciality = SpecialityEntity(title = "Ophthalmology", description = "Medical and surgical care of the eyes and visual system, from routine eye exams and prescriptions to complex eye disease and surgery.", color = "#0891B2", iconUrl = "https://unpkg.com/lucide-static@latest/icons/eye.svg"),
                        subspecialties = listOf(
                            "General Ophthalmology" to ("Routine eye exams, refractive errors, glasses and contact lens prescriptions, and basic eye disease management." to "https://unpkg.com/lucide-static@latest/icons/glasses.svg"),
                            "Retina & Vitreous" to ("Diagnosis and treatment of retinal disorders including macular degeneration, diabetic retinopathy, and retinal detachment." to "https://unpkg.com/lucide-static@latest/icons/eye.svg"),
                            "Glaucoma" to ("Management of glaucoma through medications, laser therapy, and surgery to prevent optic nerve damage and vision loss." to "https://unpkg.com/lucide-static@latest/icons/circle.svg"),
                            "Cornea & External Disease" to ("Treatment of corneal conditions including keratoconus, dry eye disease, corneal infections, and transplant surgery." to "https://unpkg.com/lucide-static@latest/icons/layers.svg"),
                            "Pediatric Ophthalmology" to ("Eye care for children including lazy eye (amblyopia), strabismus, and congenital eye conditions." to "https://unpkg.com/lucide-static@latest/icons/star.svg"),
                        )
                    ),
                    SeedEntry(
                        speciality = SpecialityEntity(title = "ENT (Otolaryngology)", description = "Specialist care for conditions affecting the ears, nose, throat, head, and neck — from infections and hearing loss to sinus disease and head and neck cancers.", color = "#B45309", iconUrl = "https://unpkg.com/lucide-static@latest/icons/ear.svg"),
                        subspecialties = listOf(
                            "General ENT" to ("Management of ear infections, sinusitis, tonsillitis, hearing loss, nasal congestion, and voice disorders." to "https://unpkg.com/lucide-static@latest/icons/shield.svg"),
                            "Rhinology & Sinus Surgery" to ("Endoscopic treatment of chronic sinusitis, nasal polyps, and skull base disorders." to "https://unpkg.com/lucide-static@latest/icons/wind.svg"),
                            "Otology & Neurotology" to ("Diagnosis and surgical management of ear diseases including hearing loss, tinnitus, vertigo, and cochlear implants." to "https://unpkg.com/lucide-static@latest/icons/activity.svg"),
                            "Head & Neck Surgery" to ("Surgical management of head and neck cancers, thyroid/parathyroid surgery, and salivary gland conditions." to "https://unpkg.com/lucide-static@latest/icons/scissors.svg"),
                            "Laryngology & Voice" to ("Treatment of voice disorders, vocal cord lesions, swallowing problems, and airway conditions." to "https://unpkg.com/lucide-static@latest/icons/mic.svg"),
                        )
                    ),
                    SeedEntry(
                        speciality = SpecialityEntity(title = "Urology", description = "Medical and surgical management of conditions affecting the urinary tract in both sexes, and the male reproductive system.", color = "#2563EB", iconUrl = "https://unpkg.com/lucide-static@latest/icons/droplet.svg"),
                        subspecialties = listOf(
                            "General Urology" to ("Management of UTIs, kidney stones, BPH, overactive bladder, and urinary incontinence." to "https://unpkg.com/lucide-static@latest/icons/shield.svg"),
                            "Urologic Oncology" to ("Surgical and non-surgical treatment of prostate, bladder, kidney, and testicular cancers." to "https://unpkg.com/lucide-static@latest/icons/microscope.svg"),
                            "Male Reproductive Medicine" to ("Evaluation and treatment of male infertility, erectile dysfunction, hypogonadism, and vasectomy." to "https://unpkg.com/lucide-static@latest/icons/user-round.svg"),
                            "Reconstructive Urology" to ("Surgical repair of urethral strictures, urinary fistulas, and other urinary tract abnormalities." to "https://unpkg.com/lucide-static@latest/icons/settings.svg"),
                        )
                    ),
                    SeedEntry(
                        speciality = SpecialityEntity(title = "Rheumatology", description = "Diagnosis and treatment of autoimmune and inflammatory conditions affecting the joints, muscles, bones, and connective tissues.", color = "#DC2626", iconUrl = "https://unpkg.com/lucide-static@latest/icons/shield.svg"),
                        subspecialties = listOf(
                            "General Rheumatology" to ("Management of rheumatoid arthritis, lupus, gout, osteoarthritis, and fibromyalgia." to "https://unpkg.com/lucide-static@latest/icons/shield.svg"),
                            "Connective Tissue Disorders" to ("Specialized care for systemic lupus erythematosus, Sjögren's syndrome, and mixed connective tissue disease." to "https://unpkg.com/lucide-static@latest/icons/layers.svg"),
                            "Vasculitis & Rare Diseases" to ("Diagnosis and immunosuppressive treatment of vasculitis, myositis, and other rare inflammatory conditions." to "https://unpkg.com/lucide-static@latest/icons/activity.svg"),
                        )
                    ),
                    SeedEntry(
                        speciality = SpecialityEntity(title = "Infectious Disease", description = "Diagnosis and treatment of infections caused by bacteria, viruses, fungi, and parasites — from tropical diseases to complex hospital-acquired infections.", color = "#059669", iconUrl = "https://unpkg.com/lucide-static@latest/icons/shield-alert.svg"),
                        subspecialties = listOf(
                            "General Infectious Disease" to ("Evaluation of fever of unknown origin, complex infections, and guidance on appropriate antibiotic use." to "https://unpkg.com/lucide-static@latest/icons/shield.svg"),
                            "HIV Medicine" to ("Comprehensive management of HIV/AIDS including antiretroviral therapy, opportunistic infections, and prevention." to "https://unpkg.com/lucide-static@latest/icons/heart.svg"),
                            "Tropical & Travel Medicine" to ("Treatment of malaria, typhoid, dengue, schistosomiasis, and other infections endemic to tropical regions." to "https://unpkg.com/lucide-static@latest/icons/globe.svg"),
                            "Hospital Epidemiology" to ("Prevention and control of healthcare-associated infections and outbreak management in clinical settings." to "https://unpkg.com/lucide-static@latest/icons/alert-triangle.svg"),
                        )
                    ),
                    SeedEntry(
                        speciality = SpecialityEntity(title = "Allergy & Immunology", description = "Evaluation and treatment of allergic diseases, immune system disorders, and hypersensitivity reactions across all organ systems.", color = "#D97706", iconUrl = "https://unpkg.com/lucide-static@latest/icons/shield-check.svg"),
                        subspecialties = listOf(
                            "General Allergy" to ("Testing and management of food allergies, environmental allergies, allergic rhinitis, and eczema." to "https://unpkg.com/lucide-static@latest/icons/shield.svg"),
                            "Asthma & Respiratory Allergy" to ("Comprehensive asthma management including allergy testing, immunotherapy, and biologic treatments." to "https://unpkg.com/lucide-static@latest/icons/wind.svg"),
                            "Primary Immunodeficiency" to ("Diagnosis and immunoglobulin replacement therapy for inherited disorders of the immune system." to "https://unpkg.com/lucide-static@latest/icons/lock.svg"),
                            "Drug Allergy & Anaphylaxis" to ("Evaluation of drug hypersensitivity reactions, penicillin allergy testing, and anaphylaxis management." to "https://unpkg.com/lucide-static@latest/icons/alert-triangle.svg"),
                        )
                    ),
                    SeedEntry(
                        speciality = SpecialityEntity(title = "Radiology", description = "Use of imaging technology — X-ray, CT, MRI, ultrasound, and nuclear medicine — to diagnose and guide treatment of disease.", color = "#475569", iconUrl = "https://unpkg.com/lucide-static@latest/icons/scan.svg"),
                        subspecialties = listOf(
                            "Diagnostic Radiology" to ("Interpretation of medical images to diagnose conditions across all organ systems." to "https://unpkg.com/lucide-static@latest/icons/eye.svg"),
                            "Interventional Radiology" to ("Image-guided minimally invasive procedures including biopsies, embolization, angioplasty, and drains." to "https://unpkg.com/lucide-static@latest/icons/zap.svg"),
                            "Neuroradiology" to ("Specialized imaging of the brain, spine, and peripheral nervous system for neurological conditions." to "https://unpkg.com/lucide-static@latest/icons/brain.svg"),
                        )
                    ),
                    SeedEntry(
                        speciality = SpecialityEntity(title = "Nutrition & Dietetics", description = "Science-based nutritional assessment and dietary guidance for disease management, weight control, and health optimization.", color = "#16A34A", iconUrl = "https://unpkg.com/lucide-static@latest/icons/salad.svg"),
                        subspecialties = listOf(
                            "Clinical Nutrition" to ("Medical nutrition therapy for chronic conditions like diabetes, kidney disease, heart disease, and cancer." to "https://unpkg.com/lucide-static@latest/icons/heart.svg"),
                            "Sports Nutrition" to ("Performance nutrition plans, hydration strategies, and recovery diets for athletes and active individuals." to "https://unpkg.com/lucide-static@latest/icons/zap.svg"),
                            "Pediatric Nutrition" to ("Age-appropriate dietary guidance for infants, children, and teens, including feeding difficulties and growth concerns." to "https://unpkg.com/lucide-static@latest/icons/star.svg"),
                        )
                    ),
                )

                for (entry in seedData) {
                    specialities.insertOne(entry.speciality)
                    val subspecialtyEntities = entry.subspecialties.map { (title, pair) ->
                        SubspecialtyEntity(
                            specialityId = entry.speciality.id,
                            title = title,
                            description = pair.first,
                            iconUrl = pair.second,
                        )
                    }
                    if (subspecialtyEntities.isNotEmpty()) {
                        subSpecialities.insertMany(subspecialtyEntities)
                    }
                }

                println("[SEED] Specialities seeded successfully")
            } catch (e: Exception) {
                println("[SEED] Failed to seed specialities: ${e.localizedMessage}")
            }
        }
    }

    private data class SeedEntry(
        val speciality: SpecialityEntity,
        val subspecialties: List<Pair<String, Pair<String, String>>>,
    )

    private fun migrateTimestamps() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val now = Clock.System.now().toString()
                val result = specialities.updateMany(
                    Filters.exists(SpecialityEntity::createdAt.name, false),
                    Updates.combine(
                        Updates.set(SpecialityEntity::createdAt.name, now),
                        Updates.set(SpecialityEntity::updatedAt.name, now),
                    )
                )
                if (result.modifiedCount > 0) {
                    println("[MIGRATE] Backfilled createdAt/updatedAt for ${result.modifiedCount} specialities")
                }
            } catch (e: Exception) {
                println("[MIGRATE] Failed to backfill speciality timestamps: ${e.localizedMessage}")
            }
        }
    }

    init {
        seed()
        migrateTimestamps()
    }

    override suspend fun createSpeciality(specialities: List<SpecialityEntity>): Resource<Nothing> =
        withContext(Dispatchers.IO) {
            try {
                val duplicates = specialities.filter { entity ->
                    this@SpecialityRepositoryImpl.specialities
                        .find(Filters.regex(SpecialityEntity::title.name, "^${entity.title}$", "i"))
                        .firstOrNull() != null
                }.map { it.title }

                if (duplicates.isNotEmpty()) {
                    return@withContext Resource.Error("Specialit${if (duplicates.size > 1) "ies" else "y"} already exist: ${duplicates.joinToString()}")
                }

                this@SpecialityRepositoryImpl.specialities.insertMany(specialities)
                Resource.Success(data = null, message = "Speciality ${if (specialities.size > 1) "ies" else "y"} created successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to create speciality")
            }
        }

    override suspend fun updateSpeciality(
        id: String,
        title: String?,
        serviceTierId: String?,
        description: String?,
        color: String?,
        iconUrl: String?,
    ): Resource<Nothing> = withContext(Dispatchers.IO) {
        try {
            val existing = specialities.find(Filters.eq(SpecialityEntity::id.name, id)).firstOrNull()
                ?: return@withContext Resource.Error("Speciality not found")

            val updates = mutableListOf<Bson>()
            title?.trim()?.takeIf { it.isNotBlank() && it != existing.title }
                ?.let { updates.add(Updates.set(SpecialityEntity::title.name, it)) }
            description?.trim()?.takeIf { it.isNotBlank() && it != existing.description }
                ?.let { updates.add(Updates.set(SpecialityEntity::description.name, it)) }
            color?.trim()?.takeIf { it.isNotBlank() && it != existing.color }
                ?.let { updates.add(Updates.set(SpecialityEntity::color.name, it)) }
            iconUrl?.trim()?.takeIf { it != existing.iconUrl }
                ?.let { updates.add(Updates.set(SpecialityEntity::iconUrl.name, it)) }
            serviceTierId?.trim()?.takeIf { it.isNotBlank() && it != existing.serviceTierId}
                ?.let { updates.add(Updates.set(SpecialityEntity::serviceTierId.name, it)) }

            if (updates.isEmpty()) return@withContext Resource.Success(data = null, message = "No changes detected")

            updates.add(Updates.set(SpecialityEntity::updatedAt.name, Clock.System.now().toString()))
            specialities.updateOne(Filters.eq(SpecialityEntity::id.name, id), Updates.combine(updates))
            Resource.Success(data = null, message = "Speciality updated successfully")
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to update speciality")
        }
    }

    override suspend fun getSpecialities(): Resource<List<SpecialityEntity>> =
        withContext(Dispatchers.IO) {
            try {
                Resource.Success(data = specialities.find().toList(), message = "Specialities retrieved successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to retrieve specialities")
            }
        }

    override suspend fun getSpecialityById(id: String): Resource<SpecialityEntity?> =
        withContext(Dispatchers.IO) {
            try {
                val entity = specialities.find(Filters.eq(SpecialityEntity::id.name, id)).firstOrNull()
                    ?: return@withContext Resource.Error("Speciality not found")
                Resource.Success(data = entity, message = "Speciality retrieved successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to retrieve speciality")
            }
        }

    override suspend fun getSpecialityByTitle(title: String): Resource<SpecialityEntity?> =
        withContext(Dispatchers.IO) {
            try {
                val entity = specialities
                    .find(Filters.regex(SpecialityEntity::title.name, "^$title$", "i"))
                    .firstOrNull()
                    ?: return@withContext Resource.Error("Speciality not found")
                Resource.Success(data = entity, message = "Speciality retrieved successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to retrieve speciality")
            }
        }

    override suspend fun getSubSpecialities(specialityId: String): Resource<List<SubspecialtyEntity>> =
        withContext(Dispatchers.IO) {
            try {
                val list = subSpecialities
                    .find(Filters.eq(SubspecialtyEntity::specialityId.name, specialityId))
                    .toList()
                Resource.Success(data = list, message = "Subspecialities retrieved successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to retrieve subspecialities")
            }
        }

    override suspend fun getSubSpeciality(id: String): Resource<SubspecialtyEntity?> =
        withContext(Dispatchers.IO) {
            try {
                val entity = subSpecialities.find(Filters.eq(SubspecialtyEntity::id.name, id)).firstOrNull()
                    ?: return@withContext Resource.Error("Subspeciality not found")
                Resource.Success(data = entity, message = "Subspeciality retrieved successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to retrieve subspeciality")
            }
        }

    override suspend fun createSubSpeciality(
        specialityId: String,
        subspecialtyEntity: SubspecialtyEntity,
    ): Resource<SubspecialtyEntity> = withContext(Dispatchers.IO) {
        try {
            specialities.find(Filters.eq(SpecialityEntity::id.name, specialityId)).firstOrNull()
                ?: return@withContext Resource.Error("Parent speciality not found")

            val duplicate = subSpecialities.find(
                Filters.and(
                    Filters.eq(SubspecialtyEntity::specialityId.name, specialityId),
                    Filters.regex(SubspecialtyEntity::title.name, "^${subspecialtyEntity.title}$", "i")
                )
            ).firstOrNull()
            if (duplicate != null) {
                return@withContext Resource.Error("Subspeciality '${subspecialtyEntity.title}' already exists under this speciality")
            }

            subSpecialities.insertOne(subspecialtyEntity)
            Resource.Success(data = subspecialtyEntity, message = "Subspeciality created successfully")
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to create subspeciality")
        }
    }

    override suspend fun updateSubSpeciality(
        id: String,
        title: String?,
        description: String?,
        color: String?,
        iconUrl: String?,
    ): Resource<SubspecialtyEntity> = withContext(Dispatchers.IO) {
        try {
            val existing = subSpecialities.find(Filters.eq(SubspecialtyEntity::id.name, id)).firstOrNull()
                ?: return@withContext Resource.Error("Subspeciality not found")

            val updates = mutableListOf<Bson>()
            title?.trim()?.takeIf { it.isNotBlank() && it != existing.title }
                ?.let { updates.add(Updates.set(SubspecialtyEntity::title.name, it)) }
            description?.trim()?.takeIf { it.isNotBlank() && it != existing.description }
                ?.let { updates.add(Updates.set(SubspecialtyEntity::description.name, it)) }
            color?.trim()?.takeIf { it.isNotBlank() && it != existing.color }
                ?.let { updates.add(Updates.set(SubspecialtyEntity::color.name, it)) }
            iconUrl?.trim()?.takeIf { it != existing.iconUrl }
                ?.let { updates.add(Updates.set(SubspecialtyEntity::iconUrl.name, it)) }

            if (updates.isEmpty()) return@withContext Resource.Success(data = existing, message = "No changes detected")

            subSpecialities.updateOne(Filters.eq(SubspecialtyEntity::id.name, id), Updates.combine(updates))
            val updated = subSpecialities.find(Filters.eq(SubspecialtyEntity::id.name, id)).firstOrNull()!!
            Resource.Success(data = updated, message = "Subspeciality updated successfully")
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to update subspeciality")
        }
    }

    override suspend fun updateSpecialityIcon(id: String, iconUrl: String?): Resource<SpecialityEntity?> =
        withContext(Dispatchers.IO) {
            try {
                specialities.find(Filters.eq(SpecialityEntity::id.name, id)).firstOrNull()
                    ?: return@withContext Resource.Error("Speciality not found")
                specialities.updateOne(
                    Filters.eq(SpecialityEntity::id.name, id),
                    Updates.combine(
                        Updates.set(SpecialityEntity::iconUrl.name, iconUrl),
                        Updates.set(SpecialityEntity::updatedAt.name, Clock.System.now().toString()),
                    )
                )
                val updated = specialities.find(Filters.eq(SpecialityEntity::id.name, id)).firstOrNull()
                Resource.Success(data = updated, message = "Icon updated successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to update speciality icon")
            }
        }

    override suspend fun updateSubSpecialityIcon(id: String, iconUrl: String?): Resource<SubspecialtyEntity?> =
        withContext(Dispatchers.IO) {
            try {
                subSpecialities.find(Filters.eq(SubspecialtyEntity::id.name, id)).firstOrNull()
                    ?: return@withContext Resource.Error("Subspeciality not found")
                subSpecialities.updateOne(
                    Filters.eq(SubspecialtyEntity::id.name, id),
                    Updates.set(SubspecialtyEntity::iconUrl.name, iconUrl)
                )
                val updated = subSpecialities.find(Filters.eq(SubspecialtyEntity::id.name, id)).firstOrNull()
                Resource.Success(data = updated, message = "Icon updated successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to update subspeciality icon")
            }
        }

    override suspend fun deleteSpeciality(id: String): Resource<Nothing> =
        withContext(Dispatchers.IO) {
            try {
                val result = specialities.deleteOne(Filters.eq(SpecialityEntity::id.name, id))
                if (result.deletedCount == 0L) return@withContext Resource.Error("Speciality not found")
                subSpecialities.deleteMany(Filters.eq(SubspecialtyEntity::specialityId.name, id))
                Resource.Success(data = null, message = "Speciality deleted successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to delete speciality")
            }
        }

    override suspend fun deleteSubSpeciality(id: String): Resource<Nothing> =
        withContext(Dispatchers.IO) {
            try {
                val result = subSpecialities.deleteOne(Filters.eq(SubspecialtyEntity::id.name, id))
                if (result.deletedCount == 0L) return@withContext Resource.Error("Subspeciality not found")
                Resource.Success(data = null, message = "Subspeciality deleted successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to delete subspeciality")
            }
        }

    override suspend fun deleteAllSubSpecialityBySpecialityId(specialityId: String): Resource<Nothing> =
        withContext(Dispatchers.IO) {
            try {
                val result = subSpecialities.deleteMany(Filters.eq(SubspecialtyEntity::specialityId.name, specialityId))
                if (result.deletedCount == 0L) return@withContext Resource.Error("No sub-specialities found for speciality")
                Resource.Success(data = null, message = "Sub-specialities deleted successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to delete sub-specialities")
            }
        }

    override suspend fun deleteSpecialityWithSubSpecialities(id: String): Resource<Nothing> =
        withContext(Dispatchers.IO) {
            val session = client.startSession()
            try {
                session.startTransaction()
                val specialityResult = specialities.deleteOne(session, Filters.eq(SpecialityEntity::id.name, id))
                if (specialityResult.deletedCount == 0L) {
                    session.abortTransaction()
                    return@withContext Resource.Error("Speciality not found")
                }
                subSpecialities.deleteMany(session, Filters.eq(SubspecialtyEntity::specialityId.name, id))
                session.commitTransaction()
                Resource.Success(data = null, message = "Speciality and its sub-specialities deleted successfully")
            } catch (e: Exception) {
                runCatching { session.abortTransaction() }
                Resource.Error(e.localizedMessage ?: "Failed to delete speciality")
            } finally {
                session.close()
            }
        }

    override suspend fun assignToServiceTier(specialityId: String, serviceTierId: String): Resource<Nothing> =
        withContext(Dispatchers.IO) {
            try {
                specialities.find(Filters.eq(SpecialityEntity::id.name, specialityId)).firstOrNull()
                    ?: return@withContext Resource.Error("Speciality not found")
                serviceTiers.find(Filters.eq(ServiceTierEntity::id.name, serviceTierId)).firstOrNull()
                    ?: return@withContext Resource.Error("Service tier not found")
                specialities.updateOne(
                    Filters.eq(SpecialityEntity::id.name, specialityId),
                    Updates.combine(
                        Updates.set(SpecialityEntity::serviceTierId.name, serviceTierId),
                        Updates.set(SpecialityEntity::updatedAt.name, Clock.System.now().toString()),
                    )
                )
                Resource.Success(data = null, message = "Speciality assigned to service tier successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to assign speciality to service tier")
            }
        }

    override suspend fun unassignFromServiceTier(specialityId: String): Resource<Nothing> =
        withContext(Dispatchers.IO) {
            try {
                specialities.find(Filters.eq(SpecialityEntity::id.name, specialityId)).firstOrNull()
                    ?: return@withContext Resource.Error("Speciality not found")
                specialities.updateOne(
                    Filters.eq(SpecialityEntity::id.name, specialityId),
                    Updates.combine(
                        Updates.unset(SpecialityEntity::serviceTierId.name),
                        Updates.set(SpecialityEntity::updatedAt.name, Clock.System.now().toString()),
                    )
                )
                Resource.Success(data = null, message = "Speciality unassigned from service tier successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to unassign speciality from service tier")
            }
        }

    override suspend fun assignToServiceCategory(specialityId: String, serviceCategoryId: String): Resource<Nothing> =
        withContext(Dispatchers.IO) {
            try {
                val speciality = specialities.find(Filters.eq(SpecialityEntity::id.name, specialityId)).firstOrNull()
                    ?: return@withContext Resource.Error("Speciality not found")
                if (speciality.serviceCategoryId == serviceCategoryId)
                    return@withContext Resource.Error("Speciality is already assigned to this service category")
                serviceCategories.find(Filters.eq(ServiceCategoryEntity::id.name, serviceCategoryId)).firstOrNull()
                    ?: return@withContext Resource.Error("Service category not found")
                specialities.updateOne(
                    Filters.eq(SpecialityEntity::id.name, specialityId),
                    Updates.combine(
                        Updates.set(SpecialityEntity::serviceCategoryId.name, serviceCategoryId),
                        Updates.set(SpecialityEntity::updatedAt.name, Clock.System.now().toString()),
                    )
                )
                Resource.Success(data = null, message = "Speciality assigned to service category successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to assign speciality to service category")
            }
        }

    override suspend fun unassignFromServiceCategory(specialityId: String): Resource<Nothing> =
        withContext(Dispatchers.IO) {
            try {
                val speciality = specialities.find(Filters.eq(SpecialityEntity::id.name, specialityId)).firstOrNull()
                    ?: return@withContext Resource.Error("Speciality not found")
                if (speciality.serviceCategoryId == null)
                    return@withContext Resource.Error("Speciality is not assigned to any service category")
                specialities.updateOne(
                    Filters.eq(SpecialityEntity::id.name, specialityId),
                    Updates.combine(
                        Updates.unset(SpecialityEntity::serviceCategoryId.name),
                        Updates.set(SpecialityEntity::updatedAt.name, Clock.System.now().toString()),
                    )
                )
                Resource.Success(data = null, message = "Speciality unassigned from service category successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to unassign speciality from service category")
            }
        }
}
