import { Injectable, inject, signal } from '@angular/core';
import { AuthService } from './auth.service';

export interface TutorialStep {
  text: string;
  pose: 1 | 2 | 3;
  targetSelector: string | null;
  reversed?: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class TutorialService {
  private authService = inject(AuthService);

  readonly activeTutorial = signal<string | null>(null);
  readonly currentStepIndex = signal<number>(0);
  readonly currentSteps = signal<TutorialStep[]>([]);

  private readonly tutorialData: Record<string, TutorialStep[]> = {
    lobby: [
      {
        pose: 1,
        targetSelector: null,
        text: '¡Hola, entrenador! Qué gusto saludarte. Soy el Profesor Pikachu y te guiaré a través del funcionamiento básico de nuestra interfaz de combate Pokémon TCG. ¡Comencemos!',
      },
      {
        pose: 2,
        targetSelector: '#btn-battle',
        text: "En el centro de tu pantalla verás el botón 'BATALLAR'. Al hacer clic, entrarás en la cola de emparejamiento clasificatorio. El sistema buscará de manera automática a un oponente que posea un nivel de habilidad (MMR) cercano al tuyo para garantizar una contienda justa.",
      },
      {
        pose: 3,
        targetSelector: '#rango-info',
        text: 'Justo debajo del botón de batalla, se indica tu rango actual en la liga. Ganar duelos incrementará tu MMR, permitiéndote ascender en los rangos competitivos y consagrarte como un auténtico Maestro Pokémon.',
        reversed: true,
      },
      {
        pose: 1,
        targetSelector: '#hud-resources',
        text: 'En la parte superior derecha de tu pantalla encontrarás el HUD de recursos. Aquí se muestran tus Puntos de Batalla y tus Pokécoins acumuladas en esta versión beta. Con ellas podrás adquirir nuevos sobres de cartas y personalizaciones en el futuro.',
      },
    ],
    deck: [
      {
        pose: 1,
        targetSelector: null,
        text: '¡Bienvenido a la Forja de Mazos, Entrenador! En esta sección podrás estructurar tus estrategias, crear nuevos mazos personalizados o modificar las barajas activas que posees para los combates.',
      },
      {
        pose: 3,
        targetSelector: '#cartas-disponibles',
        text: 'En la cuadrícula central se despliegan todas las cartas disponibles en tu colección. Cada carta detalla su tipo de energía, sus ataques y sus puntos de salud (HP) para ayudarte a planificar tu juego.',
        reversed: true,
      },
      {
        pose: 2,
        targetSelector: '#filtros-busqueda',
        text: 'Para facilitar tu búsqueda, cuentas con una barra de filtrado por energía en el costado izquierdo (Fuego, Agua, Rayo, Incolora) y un buscador de texto arriba. Así hallarás rápidamente la carta perfecta para tu combinación.',
      },
      {
        pose: 3,
        targetSelector: '#cartas-mazo',
        text: 'Haz doble clic en una carta de tu colección para sumarla a tu mazo de juego. Recuerda la regla oficial: un mazo válido para combatir debe estar conformado obligatoriamente por exactamente 60 cartas.',
      },
      {
        pose: 2,
        targetSelector: '#boton-guardar',
        text: "Una vez que tu mazo cuente con las 60 cartas requeridas, podrás presionar el botón 'Guardar' en la esquina superior derecha para asegurar tus cambios y tenerlo listo para la batalla. ¡Haz tu mejor combinación!",
      },
    ],
    profile: [
      {
        pose: 1,
        targetSelector: null,
        text: 'Esta es tu Vitrina de Entrenador. Aquí puedes examinar toda tu carrera y tus logros. Es tu carta de presentación ante la comunidad competitiva.',
      },
      {
        pose: 2,
        targetSelector: '#perfil-tabs',
        text: 'La interfaz está dividida en varias secciones clave: Vitrinas para presumir tus cartas, tus Logros desbloqueados y las Estadísticas generales de tu cuenta.',
      },
      {
        pose: 3,
        targetSelector: '#perfil-nivel',
        text: 'En el panel izquierdo se muestra tu nivel de entrenador y tu barra de experiencia (XP). Ganarás puntos de experiencia al jugar partidas y cumplir misiones. ¡Sigue progresando para obtener recompensas premium!',
      },
      {
        pose: 1,
        targetSelector: '#perfil-stats',
        text: 'En el centro se detalla tu historial estadístico de batallas: total de victorias, derrotas y tu tasa de efectividad (Win Rate). Úsalo para medir tu desempeño general.',
      },
      {
        pose: 2,
        targetSelector: '#perfil-vitrinas',
        text: "En la pestaña de 'Vitrina' puedes lucir tus cartas y mazos destacados. Haz clic en las ranuras de vitrinas para seleccionar aquellas cartas raras o especiales que quieras mostrar en tu perfil.",
      },
      {
        pose: 3,
        targetSelector: '#perfil-amigos',
        text: 'Finalmente, en la barra lateral derecha puedes gestionar tu lista de amigos y ver quiénes están conectados para chatear. Desde allí también podrás revisar tus solicitudes de amistad recibidas y buscar a otros entrenadores ingresando su nombre de usuario para enviarles una solicitud.',
        reversed: true,
      },
    ],
  };

  triggerTutorial(screen: string, force = false): void {
    const username = this.authService.username || 'guest';
    const storageKey = `tutorial_visto_${username}_${screen}`;
    const seen = localStorage.getItem(storageKey) === 'true';

    if (force || !seen) {
      const steps = this.tutorialData[screen];
      if (steps) {
        this.currentSteps.set(steps);
        this.currentStepIndex.set(0);
        this.activeTutorial.set(screen);
      }
    }
  }

  nextStep(): void {
    const currentIndex = this.currentStepIndex();
    const steps = this.currentSteps();

    if (currentIndex < steps.length - 1) {
      this.currentStepIndex.set(currentIndex + 1);
    } else {
      this.closeTutorial();
    }
  }

  closeTutorial(): void {
    const screen = this.activeTutorial();
    if (screen) {
      const username = this.authService.username || 'guest';
      const storageKey = `tutorial_visto_${username}_${screen}`;
      localStorage.setItem(storageKey, 'true');
    }
    this.activeTutorial.set(null);
    this.currentStepIndex.set(0);
    this.currentSteps.set([]);
  }
}
